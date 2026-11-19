# ICPC-System 接口冒烟测试（ASCII only，避免 Windows 控制台编码干扰）
# 前置：应用已在 http://localhost:8080 启动
# 执行：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-test.ps1

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api/v1'
$utf8 = [System.Text.Encoding]::UTF8
$script:pass = 0
$script:fail = 0

function Call($method, $path, $token, $body) {
    $p = @{ Uri = "$base$path"; Method = $method; ContentType = 'application/json; charset=utf-8' }
    if ($token) { $p.Headers = @{ Authorization = "Bearer $token" } }
    if ($body) { $p.Body = $utf8.GetBytes($body) }
    Invoke-RestMethod @p
}

function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Host "[PASS] $name" }
    else { $script:fail++; Write-Host "[FAIL] $name : $detail" }
}

function ExpectStatus($name, $expected, $scriptBlock) {
    try {
        & $scriptBlock | Out-Null
        Check $name $false "expected HTTP $expected but got 200"
    } catch {
        $code = 0
        try { $code = [int]$_.Exception.Response.StatusCode } catch { }
        Check $name ($code -eq $expected) "expected HTTP $expected but got $code"
    }
}

Write-Host "===== 1. AUTH ====="
$r = Call 'POST' '/auth/login' $null '{"studentId":"2023210100","password":"Admin@123456"}'
$admin = $r.data.token
Check 'admin login' ($r.code -eq 0 -and $admin) $r.message

$r = Call 'POST' '/auth/login' $null '{"studentId":"2023210101","password":"123456"}'
$member = $r.data.token
Check 'member login' ($r.code -eq 0 -and $member) $r.message

Write-Host "===== 2. TAG ====="
$r = Call 'GET' '/tags' $admin
Check 'tag tree readable' ($r.code -eq 0) $r.message

$r = Call 'POST' '/tags' $admin '{"name":"DP","category":"DP","sortOrder":1}'
$parentId = $r.data
Check 'create root tag' ($r.code -eq 0 -and $parentId) "id=$parentId"

$r = Call 'POST' '/tags' $admin ('{"name":"Knapsack","category":"DP","parentId":' + $parentId + '}')
$childId = $r.data
Check 'create child tag' ($r.code -eq 0 -and $childId) "id=$childId"

$r = Call 'GET' '/tags' $admin
$parentNode = $r.data | Where-Object { $_.tagId -eq $parentId } | Select-Object -First 1
$treeOk = ($parentNode -ne $null) -and ($parentNode.children.Count -eq 1)
Check 'tag tree nested' $treeOk ($r.data | ConvertTo-Json -Depth 5 -Compress)

ExpectStatus 'duplicate name rejected (409)' 409 { Call 'POST' '/tags' $admin '{"name":"DP","category":"DP"}' }
ExpectStatus 'parent cycle rejected (409)' 409 { Call 'PUT' "/tags/$parentId" $admin ('{"parentId":' + $childId + '}') }
ExpectStatus 'delete with child rejected (409)' 409 { Call 'DELETE' "/tags/$parentId" $admin $null }

$r = Call 'PUT' "/tags/$childId" $admin '{"name":"Knapsack-2"}'
Check 'update tag' ($r.code -eq 0) $r.message

$r = Call 'GET' '/tags' $admin
$parentNode = $r.data | Where-Object { $_.tagId -eq $parentId } | Select-Object -First 1
$childNode = $parentNode.children | Where-Object { $_.tagId -eq $childId } | Select-Object -First 1
Check 'update tag applied' ($childNode.name -eq 'Knapsack-2') ('name=' + $childNode.name)

$r = Call 'PUT' "/tags/$childId" $admin '{"parentId":0}'
Check 'move tag to root' ($r.code -eq 0) $r.message
$r = Call 'GET' '/tags' $admin
$moved = $r.data | Where-Object { $_.tagId -eq $childId } | Select-Object -First 1
Check 'move tag to root applied' ($moved -ne $null) ($r.data | ConvertTo-Json -Depth 5 -Compress)
$r = Call 'PUT' "/tags/$childId" $admin ('{"parentId":' + $parentId + '}')
Check 'move tag back' ($r.code -eq 0) $r.message

$r = Call 'DELETE' "/tags/$childId" $admin $null
Check 'delete child tag' ($r.code -eq 0) $r.message
$r = Call 'DELETE' "/tags/$parentId" $admin $null
Check 'delete root tag' ($r.code -eq 0) $r.message

ExpectStatus 'tag write forbidden for member (403)' 403 { Call 'POST' '/tags' $member '{"name":"X","category":"Y"}' }

Write-Host "===== 3. MBTI ====="
$r = Call 'GET' '/mbti/questions' $member
Check 'mbti question list (8)' ($r.data.Count -eq 8) "count=$($r.data.Count)"
Check 'mbti option dimension single letter' ($r.data[0].options[0].dimension -eq 'E') ('dim=' + $r.data[0].options[0].dimension)

$answers = @()
foreach ($q in $r.data) { $answers += @{ questionId = $q.questionId; option = 'A' } }
$body = (@{ answers = $answers } | ConvertTo-Json -Depth 5 -Compress)
$r = Call 'POST' '/mbti/submit' $member $body
Check 'mbti submit all-A -> ESTJ' ($r.data.type -eq 'ESTJ') ('type=' + $r.data.type)
Check 'mbti scores present' ($null -ne $r.data.scores.E_I) ('scores=' + ($r.data.scores | ConvertTo-Json -Compress))

$r = Call 'GET' '/mbti/results/2' $member
Check 'mbti latest result' ($r.data.type -eq 'ESTJ') ('type=' + $r.data.type)

ExpectStatus 'mbti duplicate answer rejected (400)' 400 { Call 'POST' '/mbti/submit' $member '{"answers":[{"questionId":1,"option":"A"},{"questionId":1,"option":"B"}]}' }
ExpectStatus 'mbti unknown question rejected (400)' 400 { Call 'POST' '/mbti/submit' $member '{"answers":[{"questionId":9999,"option":"A"}]}' }
ExpectStatus 'mbti result forbidden for others (403)' 403 { Call 'GET' '/mbti/results/1' $member $null }

Write-Host "===== 4. FILE ====="
Add-Type -AssemblyName System.Net.Http

function Upload($token, $localPath, $formName) {
    $client = New-Object System.Net.Http.HttpClient
    if ($token) {
        $client.DefaultRequestHeaders.Authorization =
            (New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $token))
    }
    $content = New-Object System.Net.Http.MultipartFormDataContent
    [byte[]]$bytes = [System.IO.File]::ReadAllBytes($localPath)
    $fileContent = [System.Net.Http.ByteArrayContent]::new($bytes)
    $content.Add($fileContent, 'file', $formName)
    $response = $client.PostAsync("$base/files/upload", $content).Result
    $status = [int]$response.StatusCode
    $body = $response.Content.ReadAsStringAsync().Result
    $client.Dispose()
    return @{ Status = $status; Body = $body }
}

$dir = Join-Path $env:TEMP 'icpc-smoke'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
$txt = Join-Path $dir 'note.txt'
Set-Content -Path $txt -Value 'hello icpc' -Encoding ASCII
$exe = Join-Path $dir 'bad.exe'
Set-Content -Path $exe -Value 'MZ' -Encoding ASCII

$res = Upload $member $txt 'note.txt'
$obj = $res.Body | ConvertFrom-Json
Check 'upload txt (200)' ($res.Status -eq 200 -and $obj.data.url) "status=$($res.Status) body=$($res.Body)"

if ($obj.data.url) {
    $download = Invoke-WebRequest -Uri ('http://localhost:8080' + $obj.data.url) -UseBasicParsing
    Check 'uploaded file downloadable' ($download.StatusCode -eq 200) "status=$($download.StatusCode)"
}

$res = Upload $member $exe 'bad.exe'
Check 'upload exe rejected (400)' ($res.Status -eq 400) "status=$($res.Status) body=$($res.Body)"

$res = Upload '' $txt 'note.txt'
Check 'upload without token rejected (401/403)' (($res.Status -eq 401) -or ($res.Status -eq 403)) "status=$($res.Status)"

Write-Host "===== 5. TEAM ====="

function EnsureMember($token, $studentId, $name) {
    try {
        $r = Call 'POST' '/members' $token ('{"studentId":"' + $studentId + '","name":"' + $name + '","major":"SE","memberType":"MEMBER"}')
        return $r.data.memberId
    } catch {
        $r = Call 'GET' ('/members?keyword=' + $studentId) $token
        return $r.data.records[0].memberId
    }
}

$m3 = EnsureMember $admin '2023210102' 'Member3'
$m4 = EnsureMember $admin '2023210103' 'Member4'
Check 'ensure test members' ($m3 -and $m4) "m3=$m3 m4=$m4"

$r = Call 'POST' '/auth/login' $null '{"studentId":"2023210102","password":"123456"}'
$member3 = $r.data.token
Check 'member3 login' ($r.code -eq 0 -and $member3) $r.message

$r = Call 'POST' '/teams' $member '{"name":"TeamA","contestTarget":"Scholastic","invitedMemberIds":[1]}'
$teamId = $r.data
Check 'create team' ($r.code -eq 0 -and $teamId) "id=$teamId"

ExpectStatus 'create team inviting self rejected (400)' 400 { Call 'POST' '/teams' $member '{"name":"SelfTeam","contestTarget":"X","invitedMemberIds":[2]}' }

$r = Call 'GET' "/teams/$teamId" $member
Check 'team detail members = 2' ($r.data.members.Count -eq 2) "count=$($r.data.members.Count)"
Check 'team detail ability summary' ($r.data.abilitySummary.memberCount -eq 2) ('memberCount=' + $r.data.abilitySummary.memberCount)
Check 'team status pending confirm' ($r.data.basicInfo.status -eq 'PENDING_CONFIRM') ('status=' + $r.data.basicInfo.status)

$r = Call 'GET' '/teams?page=1&size=10' $admin
Check 'team list' ($r.data.total -ge 1) "total=$($r.data.total)"

$r = Call 'POST' "/teams/$teamId/members" $member ('{"memberId":' + $m3 + '}')
Check 'invite third member' ($r.code -eq 0) $r.message

ExpectStatus 'invite over limit rejected (409)' 409 { Call 'POST' "/teams/$teamId/members" $member ('{"memberId":' + $m4 + '}') }
ExpectStatus 'invite duplicate rejected (409)' 409 { Call 'POST' "/teams/$teamId/members" $member '{"memberId":1}' }
ExpectStatus 'remove leader rejected (409)' 409 { Call 'DELETE' "/teams/$teamId/members?memberId=2" $member $null }
ExpectStatus 'non-leader update forbidden (403)' 403 { Call 'PUT' "/teams/$teamId" $member3 '{"name":"Hack"}' }
ExpectStatus 'non-leader remove forbidden (403)' 403 { Call 'DELETE' "/teams/$teamId/members?memberId=1" $member3 $null }

$r = Call 'PUT' "/teams/$teamId" $member ('{"leaderId":' + $m3 + '}')
Check 'transfer leader' ($r.code -eq 0) $r.message
$r = Call 'GET' "/teams/$teamId" $member
Check 'transfer leader applied' ($r.data.basicInfo.leaderId -eq $m3) ('leaderId=' + $r.data.basicInfo.leaderId)

$r = Call 'DELETE' "/teams/$teamId/members?memberId=1" $admin $null
Check 'admin remove member' ($r.code -eq 0) $r.message
$r = Call 'POST' "/teams/$teamId/members" $admin '{"memberId":1}'
Check 'admin re-invite member' ($r.code -eq 0) $r.message

$r = Call 'POST' "/teams/$teamId/confirm" $admin $null
Check 'confirm team' ($r.code -eq 0) $r.message
$r = Call 'GET' "/teams/$teamId" $admin
Check 'team confirmed' ($r.data.basicInfo.status -eq 'CONFIRMED') ('status=' + $r.data.basicInfo.status)

ExpectStatus 'invite after confirm rejected (409)' 409 { Call 'POST' "/teams/$teamId/members" $admin ('{"memberId":' + $m4 + '}') }
ExpectStatus 'confirm twice rejected (409)' 409 { Call 'POST' "/teams/$teamId/confirm" $admin $null }
ExpectStatus 'update after confirm rejected (409)' 409 { Call 'PUT' "/teams/$teamId" $member3 '{"name":"Late"}' }
ExpectStatus 'confirm forbidden for member (403)' 403 { Call 'POST' "/teams/$teamId/confirm" $member $null }

Write-Host "===== 6. STATISTICS ====="
$r = Call 'GET' '/statistics/members/overview' $admin
Check 'member overview' ($r.code -eq 0 -and $r.data.byMemberType.Count -ge 1) ('types=' + $r.data.byMemberType.Count)
ExpectStatus 'member overview forbidden for member (403)' 403 { Call 'GET' '/statistics/members/overview' $member $null }

$r = Call 'GET' '/statistics/training/summary' $admin
Check 'training summary' ($r.code -eq 0 -and $r.data.totalTrainings -ge 1) ('total=' + $r.data.totalTrainings)
ExpectStatus 'training summary forbidden for member (403)' 403 { Call 'GET' '/statistics/training/summary' $member $null }
ExpectStatus 'invalid date range rejected (400)' 400 { Call 'GET' '/statistics/training/summary?startDate=2026-12-01&endDate=2026-11-01' $admin $null }

Write-Host "===== 7. EXPORT ====="
$r = Call 'GET' '/trainings?page=1&size=20' $admin
$smokeTraining = $r.data.records | Where-Object { $_.name -eq 'SmokeTest Training' } | Select-Object -First 1
if ($smokeTraining) {
    $resp = Invoke-WebRequest -Uri ($base + '/export/training-report/' + $smokeTraining.trainingId) -Headers @{ Authorization = "Bearer $admin" } -UseBasicParsing
    Check 'export training xlsx (200)' ($resp.StatusCode -eq 200 -and $resp.Content.Length -gt 1000) ('bytes=' + $resp.Content.Length)
} else { Check 'export training xlsx (200)' $false 'SmokeTest Training not found' }

$r = Call 'GET' '/contests?page=1&size=20' $admin
$smokeContest = $r.data.records | Where-Object { $_.name -eq 'SmokeTest Contest' } | Select-Object -First 1
if ($smokeContest) {
    $resp = Invoke-WebRequest -Uri ($base + '/export/contest-certificates/' + $smokeContest.contestId) -Headers @{ Authorization = "Bearer $admin" } -UseBasicParsing
    Check 'export certificates zip (200)' ($resp.StatusCode -eq 200 -and $resp.Content.Length -gt 100) ('bytes=' + $resp.Content.Length)
} else { Check 'export certificates zip (200)' $false 'SmokeTest Contest not found' }

ExpectStatus 'export forbidden for member (403)' 403 { Invoke-WebRequest -Uri ($base + '/export/training-report/1') -Headers @{ Authorization = "Bearer $member" } -UseBasicParsing | Out-Null }
ExpectStatus 'export missing training (404)' 404 { Invoke-WebRequest -Uri ($base + '/export/training-report/999999') -Headers @{ Authorization = "Bearer $admin" } -UseBasicParsing | Out-Null }

Write-Host "===== 8. RECOMMENDATION ====="
$r = Call 'GET' '/recommendations/problems?limit=5' $member
Check 'recommend problems' ($r.code -eq 0) $r.message
$hit = @($r.data | Where-Object { $_.title -eq 'SmokeTest Problem' }).Count
Check 'recommend problems hits smoke problem' ($hit -ge 1) ('count=' + $r.data.Count)

$r = Call 'GET' '/recommendations/trainings?limit=5' $member
Check 'recommend trainings' ($r.code -eq 0 -and $r.data.Count -ge 1) ('count=' + $r.data.Count)

ExpectStatus 'recommend others forbidden (403)' 403 { Call 'GET' '/recommendations/problems?userId=1' $member $null }

Write-Host ""
Write-Host "===== RESULT: PASS=$script:pass FAIL=$script:fail ====="
if ($script:fail -gt 0) { exit 1 }
