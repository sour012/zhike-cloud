package com.zhike.notice;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.common.BizException;
import com.zhike.notice.dto.NoticeRow;
import com.zhike.notice.mapper.NoticeMapper;
import com.zhike.security.AuthUserHolder;
import com.zhike.security.LoginUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Transactional(readOnly = true)
    public List<NoticeRow> list(String keyword) {
        return noticeMapper.selectRows(keyword);
    }

    @Transactional
    public Long publish(Notice notice) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new BizException("标题不能为空");
        }
        notice.setId(null);
        notice.setPublisherId(AuthUserHolder.currentId());
        notice.setPublishedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Transactional
    public void update(Long id, Notice notice) {
        Notice db = noticeMapper.selectById(id);
        if (db == null) {
            throw new BizException(404, "公告不存在");
        }
        LoginUser me = AuthUserHolder.current();
        if (!me.isAdmin() && !db.getPublisherId().equals(me.getId())) {
            throw new BizException(403, "只能编辑自己发布的公告");
        }
        db.setTitle(notice.getTitle());
        db.setContent(notice.getContent());
        noticeMapper.updateById(db);
    }

    @Transactional
    public void delete(Long id) {
        Notice db = noticeMapper.selectById(id);
        if (db == null) {
            throw new BizException(404, "公告不存在");
        }
        LoginUser me = AuthUserHolder.current();
        if (!me.isAdmin() && !db.getPublisherId().equals(me.getId())) {
            throw new BizException(403, "只能删除自己发布的公告");
        }
        noticeMapper.deleteById(id);
    }
}
