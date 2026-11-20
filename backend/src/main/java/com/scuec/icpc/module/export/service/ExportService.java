package com.scuec.icpc.module.export.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.module.contest.entity.Contest;
import com.scuec.icpc.module.contest.entity.ContestResult;
import com.scuec.icpc.module.contest.mapper.ContestMapper;
import com.scuec.icpc.module.contest.mapper.ContestResultMapper;
import com.scuec.icpc.module.export.dto.ExportDtos;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.team.entity.Team;
import com.scuec.icpc.module.team.entity.TeamMember;
import com.scuec.icpc.module.team.mapper.TeamMapper;
import com.scuec.icpc.module.team.mapper.TeamMemberMapper;
import com.scuec.icpc.module.training.dto.TrainingDtos;
import com.scuec.icpc.module.training.entity.Training;
import com.scuec.icpc.module.training.mapper.TrainingMapper;
import com.scuec.icpc.module.training.service.TrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 导出业务：训练成绩报表（xlsx）与赛事获奖证书打包（zip）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final TrainingService trainingService;
    private final TrainingMapper trainingMapper;
    private final ContestMapper contestMapper;
    private final ContestResultMapper contestResultMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final MemberMapper memberMapper;

    /**
     * 训练成绩报表：直接复用训练榜单的计算结果，避免两套罚时算法不一致。
     */
    public ExportDtos.ExportFile trainingReport(Long trainingId) {
        Training training = trainingMapper.selectById(trainingId);
        if (training == null) {
            throw BusinessException.notFound("训练不存在");
        }
        TrainingDtos.LeaderboardVO board = trainingService.leaderboard(trainingId);
        List<TrainingDtos.LeaderboardEntry> entries = board.getEntries() == null
                ? List.of() : board.getEntries();

        Map<Long, String> studentIdMap = loadStudentIds(entries);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(200);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("训练成绩");
            String[] headers = {"名次", "学号", "姓名", "队伍", "通过题数", "罚时(分钟)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowIndex = 1;
            for (TrainingDtos.LeaderboardEntry entry : entries) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entry.getRank() == null ? 0 : entry.getRank());
                row.createCell(1).setCellValue(studentIdMap.getOrDefault(entry.getUserId(), ""));
                row.createCell(2).setCellValue(entry.getUserName() == null ? "" : entry.getUserName());
                row.createCell(3).setCellValue(entry.getTeamName() == null ? "" : entry.getTeamName());
                row.createCell(4).setCellValue(entry.getSolvedCount() == null ? 0 : entry.getSolvedCount());
                row.createCell(5).setCellValue(entry.getPenalty() == null ? 0 : entry.getPenalty());
            }

            workbook.write(out);
            log.info("导出训练 {} 成绩报表，共 {} 行", trainingId, entries.size());
            return new ExportDtos.ExportFile(training.getName() + "-成绩报表.xlsx",
                    XLSX_CONTENT_TYPE, out.toByteArray());
        } catch (IOException ex) {
            log.error("生成训练报表失败：{}", trainingId, ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "生成训练报表失败");
        }
    }

    /**
     * 赛事证书打包：会长/副会长导出全部，其他会员只导出本人的证书。
     * 每个获奖记录生成一个文本证书，后续可替换为 PDF 模板。
     */
    public ExportDtos.ExportFile contestCertificates(Long contestId, Long requesterId, boolean includeAll) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw BusinessException.notFound("赛事不存在");
        }
        List<ContestResult> results = contestResultMapper.selectList(
                Wrappers.<ContestResult>lambdaQuery()
                        .eq(ContestResult::getContestId, contestId)
                        .orderByAsc(ContestResult::getRankNo)
                        .orderByAsc(ContestResult::getResultId));
        if (results.isEmpty()) {
            throw BusinessException.notFound("该赛事暂无成绩记录，无法生成证书");
        }
        if (!includeAll) {
            Set<Long> myTeamIds = teamMemberMapper.selectList(
                            Wrappers.<TeamMember>lambdaQuery()
                                    .eq(TeamMember::getMemberId, requesterId))
                    .stream().map(TeamMember::getTeamId).collect(Collectors.toSet());
            results = results.stream()
                    .filter(result -> requesterId.equals(result.getMemberId())
                            || (result.getTeamId() != null && myTeamIds.contains(result.getTeamId())))
                    .toList();
            if (results.isEmpty()) {
                throw BusinessException.notFound("你没有该赛事的获奖证书");
            }
        }

        Set<Long> teamIds = results.stream().map(ContestResult::getTeamId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> memberIds = results.stream().map(ContestResult::getMemberId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> teamNames = teamIds.isEmpty() ? Map.of()
                : teamMapper.selectBatchIds(teamIds).stream()
                        .collect(Collectors.toMap(Team::getTeamId, Team::getName, (a, b) -> a));
        Map<Long, String> memberNames = memberIds.isEmpty() ? Map.of()
                : memberMapper.selectBatchIds(memberIds).stream()
                        .collect(Collectors.toMap(Member::getMemberId, Member::getName, (a, b) -> a));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            int index = 1;
            for (ContestResult result : results) {
                String owner = result.getTeamId() != null
                        ? teamNames.getOrDefault(result.getTeamId(), "队伍" + result.getTeamId())
                        : memberNames.getOrDefault(result.getMemberId(), "会员" + result.getMemberId());
                String entryName = String.format("%02d-%s.txt", index++, safeName(owner));
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(buildCertificate(contest, result, owner).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            log.info("导出赛事 {} 证书 {} 份", contestId, results.size());
            return new ExportDtos.ExportFile(contest.getName() + "-证书.zip",
                    "application/zip", out.toByteArray());
        } catch (IOException ex) {
            log.error("生成赛事证书失败：{}", contestId, ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "生成赛事证书失败");
        }
    }

    // ==================== 内部工具 ====================

    private Map<Long, String> loadStudentIds(List<TrainingDtos.LeaderboardEntry> entries) {
        List<Long> userIds = entries.stream().map(TrainingDtos.LeaderboardEntry::getUserId)
                .filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return memberMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(Member::getMemberId,
                        member -> member.getStudentId() == null ? "" : member.getStudentId(),
                        (a, b) -> a));
    }

    private String buildCertificate(Contest contest, ContestResult result, String owner) {
        return "获奖证书\n\n"
                + "赛事：" + contest.getName() + "\n"
                + "获奖者：" + owner + "\n"
                + "奖项：" + (result.getAward() == null ? "未设置" : result.getAward()) + "\n"
                + "名次：第 " + (result.getRankNo() == null ? "-" : result.getRankNo()) + " 名\n"
                + "通过题数：" + (result.getSolvedCount() == null ? 0 : result.getSolvedCount()) + "\n"
                + "罚时：" + (result.getPenalty() == null ? 0 : result.getPenalty()) + " 分钟\n\n"
                + "ICPC 协会\n";
    }

    /**
     * 去掉文件名中的非法字符，避免 ZipEntry 名称异常。
     */
    private String safeName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
