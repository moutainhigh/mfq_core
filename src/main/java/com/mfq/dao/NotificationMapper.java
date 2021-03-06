package com.mfq.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.mfq.annotation.MFQDao;
import com.mfq.bean.notification.Notification;
import org.springframework.stereotype.Component;

@MFQDao
@Component
public interface NotificationMapper {
    
    public List<Notification> queryNotificationByUid(@Param("start")int start, @Param("size")int size, @Param("uid") long uid, @Param("type") int type);
    
    public List<Notification> queryNotificationByType(@Param("start")int start, @Param("size")int size,@Param("type") int type);
    
    public int updateNotificationStatus(@Param("msgId") long msg_id, @Param("status") int status);

	public long queryNotificationCountByType(@Param("uid") long uid, @Param("type")int type);

    public List<Notification> queryNotificationByTypeAndUid(@Param("type") int i,@Param("uid") long uid);

    public List<Notification> queryAll(@Param("uid") long uid,@Param("start")int start, @Param("pagesize")int pagesize);

    public long queryCountAll(@Param("uid") long uid);
}
