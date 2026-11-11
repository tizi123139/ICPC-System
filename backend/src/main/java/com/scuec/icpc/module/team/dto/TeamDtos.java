package com.scuec.icpc.module.team.dto;

import com.scuec.icpc.common.api.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组队模块请求与响应对象。
 */
public final class TeamDtos {

    private TeamDtos() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        private String status;

        /**
         * 目标赛事 ID。team 表当前只有 contest_target 文本字段，暂无法按 ID 精确过滤。
         */
        private Long contestId;
    }

    @Data
    public static class CreateRequest {

        @NotBlank(message = "队伍名称不能为空")
        @Size(max = 100, message = "队伍名称长度不能超过 100")
        private String name;

        @NotBlank(message = "目标赛事不能为空")
        @Size(max = 100, message = "目标赛事长度不能超过 100")
        private String contestTarget;

        @Size(max = 500, message = "队伍简介长度不能超过 500")
        private String description;

        /**
         * 创建时一并邀请的成员，不含队长本人。
         */
        @Size(max = 2, message = "队伍最多 3 人，创建时最多邀请 2 人")
        private List<Long> invitedMemberIds;
    }

    @Data
    public static class UpdateRequest {

        @Size(max = 100, message = "队伍名称长度不能超过 100")
        private String name;

        /**
         * 转让队长时填写，新队长必须是队内成员。
         */
        private Long leaderId;

        @Size(max = 100, message = "目标赛事长度不能超过 100")
        private String contestTarget;

        @Size(max = 500, message = "队伍简介长度不能超过 500")
        private String description;
    }

    @Data
    public static class MemberRequest {
        @NotNull(message = "成员 ID 不能为空")
        private Long memberId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamVO {
        private Long teamId;
        private String name;
        private Long leaderId;
        private List<Long> memberIds;
        private String contestTarget;
        private String description;
        private String status;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberVO {
        private Long memberId;
        private String name;
        private String studentId;
        private String major;
        private String grade;
        private String memberType;
        private String department;
        private LocalDateTime joinTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagScoreVO {
        private Long tagId;
        private String tagName;
        private Integer solvedCount;
    }

    /**
     * 队伍能力画像。判题未接入时 totalSolved 恒为 0、topTags 为空。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilitySummaryVO {
        private Integer memberCount;
        private Integer totalSolved;
        private List<TagScoreVO> topTags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamDetailVO {
        private TeamVO basicInfo;
        private List<TeamMemberVO> members;
        private AbilitySummaryVO abilitySummary;
    }
}
