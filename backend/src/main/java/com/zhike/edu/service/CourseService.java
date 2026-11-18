package com.zhike.edu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhike.common.BizException;
import com.zhike.common.PageResult;
import com.zhike.edu.entity.Course;
import com.zhike.edu.entity.CourseOffering;
import com.zhike.edu.mapper.CourseMapper;
import com.zhike.edu.mapper.OfferingMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final OfferingMapper offeringMapper;

    public CourseService(CourseMapper courseMapper, OfferingMapper offeringMapper) {
        this.courseMapper = courseMapper;
        this.offeringMapper = offeringMapper;
    }

    @Transactional(readOnly = true)
    public PageResult<Course> page(String keyword, String category, long pageNo, long pageSize) {
        IPage<Course> page = courseMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<Course>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Course::getName, keyword).or().like(Course::getCode, keyword))
                        .eq(StringUtils.hasText(category), Course::getCategory, category)
                        .orderByAsc(Course::getCode));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Transactional(readOnly = true)
    public List<Course> listAll() {
        return courseMapper.selectList(Wrappers.<Course>lambdaQuery().orderByAsc(Course::getCode));
    }

    @Transactional
    public Long create(Course c) {
        validate(c);
        Long dup = courseMapper.selectCount(Wrappers.<Course>lambdaQuery().eq(Course::getCode, c.getCode()));
        if (dup != null && dup > 0) {
            throw new BizException(409, "课程编号已存在");
        }
        c.setId(null);
        courseMapper.insert(c);
        return c.getId();
    }

    @Transactional
    public void update(Long id, Course c) {
        Course db = courseMapper.selectById(id);
        if (db == null) {
            throw new BizException(404, "课程不存在");
        }
        validate(c);
        Long dup = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getCode, c.getCode()).ne(Course::getId, id));
        if (dup != null && dup > 0) {
            throw new BizException(409, "课程编号已被其他课程占用");
        }
        c.setId(id);
        courseMapper.updateById(c);
    }

    @Transactional
    public void delete(Long id) {
        Long used = offeringMapper.selectCount(
                Wrappers.<CourseOffering>lambdaQuery().eq(CourseOffering::getCourseId, id));
        if (used != null && used > 0) {
            throw new BizException("该课程已有开课记录，不能删除；如需停开请关闭对应开课");
        }
        courseMapper.deleteById(id);
    }

    private void validate(Course c) {
        if (!StringUtils.hasText(c.getCode()) || !StringUtils.hasText(c.getName())) {
            throw new BizException("课程编号与名称不能为空");
        }
        if (c.getCredit() == null || c.getCredit().signum() <= 0) {
            throw new BizException("学分必须大于 0");
        }
    }
}
