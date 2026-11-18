package com.zhike.edu;

/** 上课时间的展示与冲突判断。抽出来是为了后端校验与前端展示用同一套规则。 */
public final class TimeSlot {

    private static final String[] WEEK = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private TimeSlot() {
    }

    public static String text(Integer weekday, Integer start, Integer end) {
        if (weekday == null || start == null || end == null) {
            return "";
        }
        String day = weekday >= 1 && weekday <= 7 ? WEEK[weekday] : "第" + weekday + "天";
        return start.equals(end) ? day + " 第" + start + "节" : day + " 第" + start + "-" + end + "节";
    }

    /** 同一天且节次区间有交集才算冲突（同一门课不同班次不算） */
    public static boolean overlap(Integer w1, Integer s1, Integer e1, Integer w2, Integer s2, Integer e2) {
        if (w1 == null || w2 == null || s1 == null || e1 == null || s2 == null || e2 == null) {
            return false;
        }
        return w1.equals(w2) && s1 <= e2 && s2 <= e1;
    }
}
