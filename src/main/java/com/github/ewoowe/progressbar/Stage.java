package com.github.ewoowe.progressbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表示一个步骤
 * 其可以包含n个子步骤，子步骤必须在父步骤成功执行后才能执行，否则抛出异常；
 * 父步骤执行后可以再新增加子步骤，此时进度数值可能会出现反复增加或者减少的情况；
 * 父步骤执行失败或者取消后，无法再增加子步骤，否则抛出异常；
 * 父步骤执行失败后，会连带取消所有子步骤；
 *
 * @author wangcheng@ictnj.ac.cn
 * @since 2023/9/19
 */
public class Stage {

    /**
     * 步骤名称
     */
    private final String name;

    /**
     * 步骤属性集
     */
    private final Map<String, Object> args;

    /**
     * 当前步骤的父步骤
     */
    private Stage parentStage = null;

    /**
     * 表示当前该步骤的进度向量，默认为1
     */
    private final AtomicInteger progressVector = new AtomicInteger(1);

    /**
     * 表示以当前步骤为根节点的树总的进度向量
     */
    private final AtomicInteger treeProgressVector = new AtomicInteger(1);

    /**
     * 当前步骤的状态
     */
    private volatile StageState stageState = StageState.Initial;

    /**
     * 所有子步骤
     */
    private final List<Stage> subStages = new ArrayList<>();

    public Stage(String name, Map<String, Object> args, int vector) {
        assert vector >= 0;
        this.name = name;
        this.args = args;
        progressVector.set(vector);
        treeProgressVector.set(vector);
    }

    public Stage(String name, Map<String, Object> args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public Stage getParentStage() {
        return parentStage;
    }

    public void setParentStage(Stage parentStage) {
        this.parentStage = parentStage;
    }

    private void setStageState(StageState stageState) {
        this.stageState = stageState;
    }

    private StageState getStageState() {
        return stageState;
    }

    // TODO 暴露有风险
    public AtomicInteger getTreeProgressVector() {
        return treeProgressVector;
    }

    // TODO 暴露有风险
    public AtomicInteger getProgressVector() {
        return progressVector;
    }

    // TODO 暴露有风险
    public List<Stage> getSubStages() {
        return subStages;
    }

    private ProgressBar getProgressBar() throws ProgressBarException {
        Stage progressBar = this;
        while (!(progressBar instanceof ProgressBar)) {
            progressBar = progressBar.parentStage;
            if (progressBar == null)
                throw new ProgressBarException("stage tree not attached to a ProgressBar");
        }
        return (ProgressBar) progressBar;
    }

    public void addSubStage(Stage subStage) throws ProgressBarException {
        synchronized (getProgressBar()) {
            if (stageState == StageState.Fail || stageState == StageState.Cancel)
                throw new ProgressBarException("can not add subStage when parent stage fail or cancel");
            if (subStage != null) {
                subStages.add(subStage);
                subStage.setParentStage(this);
                calculateTreeProgressVector(subStage);
            }
        }
    }

    private void calculateTreeProgressVector(Stage subStage) throws ProgressBarException {
        if (subStage.getProgressVector().get() > 0) {
            treeProgressVector.addAndGet(subStage.getProgressVector().get());
            if (parentStage != null)
                parentStage.calculateTreeProgressVector(subStage);
        }
    }

    /**
     * 步骤完成
     */
    public  void stageDone() throws ProgressBarException {
        ProgressBar progressBar = getProgressBar();
        synchronized (progressBar) {
            if (stageState != StageState.Initial)
                throw new ProgressBarException("stage was completed, can not done again");
            if (parentStage != null && parentStage.getStageState() != StageState.Done)
                throw new ProgressBarException("parentStage not complete or fail or cancel, subStage can not done now");
            stageState = StageState.Done;
            progressBar.notifyStageDone(this);
        }
    }

    private void cancel() {
        stageState = StageState.Cancel;
        subStages.forEach(Stage::cancel);
    }

    public List<Stage> getTreeSubStages() {
        List<Stage> stages = new ArrayList<>();
        stages.add(this);
        subStages.forEach(subStage -> {
            stages.addAll(subStage.getTreeSubStages());
        });
        return stages;
    }

    /**
     * 步骤失败
     */
    public  void stageFail() throws ProgressBarException {
        ProgressBar progressBar = getProgressBar();
        synchronized (progressBar) {
            if (stageState != StageState.Initial)
                throw new ProgressBarException("stage was completed, can not fail again");
            if (parentStage != null && parentStage.getStageState() != StageState.Done)
                throw new ProgressBarException("parentStage not complete or fail or cancel, subStage can not fail now");
            stageState = StageState.Fail;
            subStages.forEach(Stage::cancel);
            progressBar.notifyStageFail(this);
        }
    }
}
