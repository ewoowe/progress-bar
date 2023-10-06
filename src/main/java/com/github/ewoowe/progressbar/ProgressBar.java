package com.github.ewoowe.progressbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进度条主类
 *
 * @author wangcheng@ictnj.ac.cn
 * @since 2023/9/19
 */
public class ProgressBar extends Stage {
    // name为进度条名称

    /**
     * 进度改变监听器
     */
    private final ProgressChangeListener progressChangeListener;

    /**
     * 已成功执行完成的进度向量
     */
    private final AtomicInteger doneProgressVector = new AtomicInteger(0);

    /**
     * 已执行失败以及所有被取消的子步骤的进度向量之和
     */
    private final AtomicInteger failedProgressVector = new AtomicInteger(0);

    /**
     * 所有先后完成的步骤
     */
    private final List<Stage> completedStages = Collections.synchronizedList(new ArrayList<>());

    public AtomicInteger getDoneProgressVector() {
        return doneProgressVector;
    }

    public AtomicInteger getFailedProgressVector() {
        return failedProgressVector;
    }

    public List<Stage> getCompletedStages() {
        return completedStages;
    }

    public ProgressBar(String name, ProgressChangeListener progressChangeListener) {
        super(name, null, 0);
        assert progressChangeListener != null;
        this.progressChangeListener = progressChangeListener;
    }

    /**
     * 进度条开启
     */
    public void start() throws ProgressBarException {
        stageDone();
    }

    public void notifyStageDone(Stage stage) {
        doneProgressVector.addAndGet(stage.getProgressVector().get());
        completedStages.add(stage);
        progressChangeListener.doneNotify(doneProgressVector.get(),
                getTreeProgressVector().get(),
                failedProgressVector.get(),
                stage);
    }

    public void notifyStageFail(Stage stage) {
        failedProgressVector.addAndGet(stage.getTreeProgressVector().get());
        completedStages.addAll(stage.getTreeSubStages());
        progressChangeListener.failedNotify(doneProgressVector.get(),
                getTreeProgressVector().get(),
                failedProgressVector.get(),
                stage);
    }
}
