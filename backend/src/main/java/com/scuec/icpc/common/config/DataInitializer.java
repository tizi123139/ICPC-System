package com.scuec.icpc.common.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.constant.Roles;
import com.scuec.icpc.module.member.entity.Member;
import com.scuec.icpc.module.member.entity.MemberRole;
import com.scuec.icpc.module.member.entity.Role;
import com.scuec.icpc.module.mbti.entity.MbtiQuestion;
import com.scuec.icpc.module.mbti.mapper.MbtiQuestionMapper;
import com.scuec.icpc.module.member.mapper.MemberMapper;
import com.scuec.icpc.module.member.mapper.MemberRoleMapper;
import com.scuec.icpc.module.member.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 开发环境初始化：写入角色字典与两个测试账号。
 * 生产环境通过 app.init-data=false 关闭。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app", name = "init-data", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private final RoleMapper roleMapper;
    private final MemberMapper memberMapper;
    private final MemberRoleMapper memberRoleMapper;
    private final MbtiQuestionMapper mbtiQuestionMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initRoles();
        initMember("2023210100", "Admin@123456", "系统管理员", Roles.PRESIDENT);
        initMember("2023210101", "123456", "测试会员", Roles.MEMBER);
        initMbtiQuestions();
        log.info("初始化数据完成。管理员账号：2023210100 / Admin@123456，测试会员：2023210101 / 123456");
    }

    private void initRoles() {
        for (String code : Roles.ALL) {
            if (roleMapper.selectById(code) == null) {
                Role role = new Role();
                role.setRoleCode(code);
                role.setRoleName(Roles.displayName(code));
                role.setDescription(Roles.displayName(code));
                roleMapper.insert(role);
            }
        }
    }

    private void initMember(String studentId, String rawPassword, String name, String roleCode) {
        Long count = memberMapper.selectCount(Wrappers.<Member>lambdaQuery()
                .eq(Member::getStudentId, studentId));
        if (count != null && count > 0) {
            return;
        }
        Member member = new Member();
        member.setStudentId(studentId);
        member.setPassword(passwordEncoder.encode(rawPassword));
        member.setName(name);
        member.setMajor("计算机科学与技术");
        member.setGrade("2023级");
        member.setMemberType(roleCode.equals(Roles.MEMBER) ? "MEMBER" : "LAB_MEMBER");
        member.setDepartment("技术部");
        member.setJoinDate(LocalDate.now());
        member.setStatus("ACTIVE");
        memberMapper.insert(member);

        MemberRole memberRole = new MemberRole();
        memberRole.setMemberId(member.getMemberId());
        memberRole.setRoleCode(roleCode);
        memberRoleMapper.insert(memberRole);
    }

    private void initMbtiQuestions() {
        Long count = mbtiQuestionMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        String[][] questions = {
                {"E_I", "参加团队讨论时，你通常？", "主动表达自己的想法", "先倾听他人再补充"},
                {"E_I", "训练结束后，你更倾向于？", "和队友继续交流复盘", "独自安静地整理思路"},
                {"S_N", "学习新算法时，你更关注？", "具体实现与代码细节", "整体思想与扩展应用"},
                {"S_N", "面对一道新题，你通常先？", "分析样例与数据范围", "联想相关模型与套路"},
                {"T_F", "队友代码出现问题时，你更可能？", "直接指出逻辑错误", "先照顾对方情绪再沟通"},
                {"T_F", "做训练计划时，你更看重？", "目标完成率与效率", "团队成员的感受与状态"},
                {"J_P", "面对比赛前的准备，你倾向于？", "提前制定详细计划", "保留弹性，随机应变"},
                {"J_P", "训练任务截止前，你通常？", "提前完成并检查", "临近截止时集中冲刺"}
        };
        int no = 1;
        for (String[] question : questions) {
            MbtiQuestion entity = new MbtiQuestion();
            entity.setQuestionNo(no++);
            entity.setDimension(question[0]);
            entity.setContent(question[1]);
            entity.setOptionA(question[2]);
            entity.setOptionB(question[3]);
            entity.setScoreA(1);
            entity.setScoreB(1);
            mbtiQuestionMapper.insert(entity);
        }
    }
}
