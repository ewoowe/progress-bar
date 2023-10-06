package com.github.ewoowe.progressbar;

/**
 * 进度状态更改通知与监听
 *
 * @author wangcheng@ictnj.ac.cn
 * @since 2023/9/19
 */
public interface ProgressChangeListener {

    /**
     * 当一个步骤成功完成时，通知监听者进度状态已改变
     *
     * @param doneProgressVector 已成功完成的进度向量
     * @param totalProgressVector 进度条总的进度向量
     * @param failedProgressVector 失败的进度向量，包括子步骤被取消的进度向量
     * @param doneStage 本次成功完成的步骤
     */
    void doneNotify(int doneProgressVector,
                int totalProgressVector,
                int failedProgressVector,
                Stage doneStage);

    /**
     * 当一个步骤失败时，通知监听者进度状态已改变
     *
     * @param doneProgressVector 已成功完成的进度向量
     * @param totalProgressVector 进度条总的进度向量
     * @param failedProgressVector 失败的进度向量，包括子步骤被取消的进度向量
     * @param failedParentStage 本次失败的步骤，该步骤的所有的子步骤被取消，并且全部增加到失败的进度向量中
     */
    void failedNotify(int doneProgressVector,
                      int totalProgressVector,
                      int failedProgressVector,
                      Stage failedParentStage);
}
