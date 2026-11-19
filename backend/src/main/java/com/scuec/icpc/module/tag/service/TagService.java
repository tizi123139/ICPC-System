package com.scuec.icpc.module.tag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scuec.icpc.common.exception.BusinessException;
import com.scuec.icpc.module.tag.dto.TagDtos;
import com.scuec.icpc.module.tag.entity.Tag;
import com.scuec.icpc.module.tag.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识点业务：树形查询与增删改。
 * 知识点是能力雷达与推荐模块的基础数据，删除时会校验子节点与题目引用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    // ==================== 查询 ====================

    /**
     * 一次性取出全部知识点后在内存中组树，避免逐层查询造成 N+1。
     */
    public List<TagDtos.TagVO> tree() {
        List<Tag> tags = tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                .orderByAsc(Tag::getSortOrder)
                .orderByAsc(Tag::getTagId));
        if (tags.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Tag>> childrenMap = tags.stream()
                .filter(tag -> tag.getParentId() != null)
                .collect(Collectors.groupingBy(Tag::getParentId));
        Set<Long> allIds = tags.stream().map(Tag::getTagId).collect(Collectors.toSet());
        // 父节点已被删除的历史数据按根节点处理，避免整棵子树消失
        return tags.stream()
                .filter(tag -> tag.getParentId() == null || !allIds.contains(tag.getParentId()))
                .map(tag -> toVO(tag, childrenMap))
                .toList();
    }

    private TagDtos.TagVO toVO(Tag tag, Map<Long, List<Tag>> childrenMap) {
        List<TagDtos.TagVO> children = childrenMap.getOrDefault(tag.getTagId(), List.of())
                .stream()
                .map(child -> toVO(child, childrenMap))
                .toList();
        return TagDtos.TagVO.builder()
                .tagId(tag.getTagId())
                .name(tag.getName())
                .category(tag.getCategory())
                .parentId(tag.getParentId())
                .sortOrder(tag.getSortOrder())
                .children(children)
                .build();
    }

    // ==================== 写操作 ====================

    @Transactional
    public Long create(TagDtos.CreateRequest request) {
        Long parentId = normalizeParentId(request.getParentId());
        if (parentId != null) {
            getOrThrow(parentId);
        }
        String name = request.getName().trim();
        ensureNameUnique(parentId, name, null);

        Tag tag = new Tag();
        tag.setName(name);
        tag.setCategory(request.getCategory().trim());
        tag.setParentId(parentId);
        tag.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        tagMapper.insert(tag);
        log.info("新增知识点 {}（{}），父节点 {}", tag.getTagId(), name, parentId);
        return tag.getTagId();
    }

    @Transactional
    public void update(Long tagId, TagDtos.UpdateRequest request) {
        Tag tag = getOrThrow(tagId);
        Long parentId = request.getParentId() == null
                ? tag.getParentId() : normalizeParentId(request.getParentId());
        if (parentId != null && !parentId.equals(tag.getParentId())) {
            if (parentId.equals(tagId)) {
                throw BusinessException.conflict("父知识点不能是自身");
            }
            getOrThrow(parentId);
            if (descendantIds(tagId).contains(parentId)) {
                throw BusinessException.conflict("父知识点不能是自己的子节点");
            }
        }
        String name = StringUtils.hasText(request.getName())
                ? request.getName().trim() : tag.getName();
        ensureNameUnique(parentId, name, tagId);

        // updateById 会忽略 null 字段，移动回根节点必须用 UpdateWrapper 显式 set
        tagMapper.update(null, Wrappers.<Tag>lambdaUpdate()
                .set(Tag::getName, name)
                .set(StringUtils.hasText(request.getCategory()),
                        Tag::getCategory, request.getCategory() == null ? null : request.getCategory().trim())
                .set(Tag::getParentId, parentId)
                .set(request.getSortOrder() != null, Tag::getSortOrder, request.getSortOrder())
                // UpdateWrapper 不带实体，审计字段的自动填充不会触发，需手动刷新
                .set(Tag::getUpdateTime, LocalDateTime.now())
                .eq(Tag::getTagId, tagId));
        log.info("修改知识点 {}，父节点 {} -> {}", tagId, tag.getParentId(), parentId);
    }

    @Transactional
    public void delete(Long tagId) {
        getOrThrow(tagId);
        Long children = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getParentId, tagId));
        if (children != null && children > 0) {
            throw BusinessException.conflict("该知识点下存在子知识点，请先删除子节点");
        }
        long linked = tagMapper.countProblemLinks(tagId);
        if (linked > 0) {
            throw BusinessException.conflict("该知识点下仍有 " + linked + " 道题目，无法删除");
        }
        tagMapper.deleteById(tagId);
        log.info("删除知识点 {}", tagId);
    }

    // ==================== 内部工具 ====================

    private Tag getOrThrow(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw BusinessException.notFound("知识点不存在");
        }
        return tag;
    }

    /**
     * 前端约定：parentId 传 0 表示根节点。
     */
    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? null : parentId;
    }

    private void ensureNameUnique(Long parentId, String name, Long excludeTagId) {
        LambdaQueryWrapper<Tag> wrapper = Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getName, name);
        if (parentId == null) {
            wrapper.isNull(Tag::getParentId);
        } else {
            wrapper.eq(Tag::getParentId, parentId);
        }
        if (excludeTagId != null) {
            wrapper.ne(Tag::getTagId, excludeTagId);
        }
        Long count = tagMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.conflict("同级下已存在同名知识点：" + name);
        }
    }

    /**
     * 收集指定知识点的全部后代 ID，用于阻止修改父节点时形成环。
     */
    private Set<Long> descendantIds(Long tagId) {
        List<Tag> all = tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                .select(Tag::getTagId, Tag::getParentId));
        Map<Long, List<Long>> childMap = all.stream()
                .filter(tag -> tag.getParentId() != null)
                .collect(Collectors.groupingBy(Tag::getParentId,
                        Collectors.mapping(Tag::getTagId, Collectors.toList())));
        Set<Long> result = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>(childMap.getOrDefault(tagId, List.of()));
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!result.add(current)) {
                continue;
            }
            queue.addAll(childMap.getOrDefault(current, List.of()));
        }
        return result;
    }
}
