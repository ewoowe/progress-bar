package com.github.ewoowe.progressbar;

/**
 * 步骤执行状态枚举
 *
 * @author wangcheng@ictnj.ac.cn
 * @since 2023/9/19
 */
public enum StageState {
    /**
     * 未执行
     */
    Initial,
    /**
     * 执行失败
     */
    Fail,
    /**
     * 跟随祖先步骤的失败而取消执行
     */
    Cancel,
    /**
     * 执行完成
     */
    Done,
}
