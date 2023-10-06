package com.github.ewoowe.progressbar;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for progress-bar.
 */
public class AppTest 
{

    public static class Listener implements ProgressChangeListener {

        @Override
        public void doneNotify(int doneProgressVector,
                               int totalProgressVector,
                               int failedProgressVector,
                               Stage doneStage) {
            System.out.println(doneStage.getName() + " done notify"
                    + ",done=" + doneProgressVector
                    + ",fail=" + failedProgressVector
                    + ",total=" + totalProgressVector);
        }

        @Override
        public void failedNotify(int doneProgressVector,
                                 int totalProgressVector,
                                 int failedProgressVector,
                                 Stage failedParentStage) {
            System.out.println(failedParentStage.getName() + " failed notify"
                    + ",done=" + doneProgressVector
                    + ",fail=" + failedProgressVector
                    + ",total=" + totalProgressVector);
        }
    }

    @Test
    public void testDetachedStage1() {
        try {
            Stage stage1 = new Stage("1", null);
            stage1.stageDone();
        } catch (ProgressBarException e) {
            e.printStackTrace();
            Assert.assertTrue(true);
            return;
        }
        Assert.fail();
    }

    @Test
    public void testSubStageDoneFirst() {
        try {
            ProgressBar progressBar = new ProgressBar("端到端网络构建任务进度条", new Listener());
            Stage stage1 = new Stage("1", null);
            progressBar.addSubStage(stage1);
            stage1.stageDone();
            progressBar.start();
        } catch (ProgressBarException e) {
            e.printStackTrace();
            Assert.assertTrue(true);
            return;
        }
        Assert.fail();
    }

    @Test
    public void testDoneAgain() {
        try {
            ProgressBar progressBar = new ProgressBar("端到端网络构建任务进度条", new Listener());
            Stage stage1 = new Stage("1", null);
            progressBar.addSubStage(stage1);
            progressBar.start();
            stage1.stageDone();
            stage1.stageFail();
        } catch (ProgressBarException e) {
            e.printStackTrace();
            Assert.assertTrue(true);
            return;
        }
        Assert.fail();
    }

    @Test
    public void testDoneSecond() throws ProgressBarException {
        ProgressBar progressBar = new ProgressBar("端到端网络构建任务进度条", new Listener());
        Stage stage1 = new Stage("1", null);
        Stage stage2 = new Stage("2", null);
        progressBar.addSubStage(stage1);
        stage1.addSubStage(stage2);
        progressBar.start();
        stage1.stageDone();
        stage2.stageFail();
        Assert.assertTrue(true);
    }

    @Test
    public void testAddAfterDone() throws ProgressBarException {
        ProgressBar progressBar = new ProgressBar("端到端网络构建任务进度条", new Listener());
        Stage stage1 = new Stage("1", null);
        Stage stage2 = new Stage("2", null);
        progressBar.addSubStage(stage1);
        progressBar.start();
        stage1.stageDone();
        stage1.addSubStage(stage2);
        stage2.stageFail();
        Assert.assertTrue(true);
    }

    private int leaves(int depth, int width) {
        int sum = 0;
        for (int i = 0; i < depth; i++)
            sum += Math.pow(width, i + 1);
        return sum;
    }

    @Test
    public void test() throws ProgressBarException {
        int depth = 3;
        int width = 10;
        int leaves = leaves(depth, width);
        CountDownLatch mainWait = new CountDownLatch(leaves);

        AtomicInteger stageId = new AtomicInteger(1);
        ProgressBar progressBar = new ProgressBar("端到端网络构建任务进度条", new Listener());
        CountDownLatch progressBarCdl = new CountDownLatch(1);
        List<Stage> parentStages = new ArrayList<>();
        parentStages.add(progressBar);
        List<CountDownLatch> parentStageLatches = new ArrayList<>();
        parentStageLatches.add(progressBarCdl);
        for (int i = 0; i < depth; i++) {
            List<Stage> stagesOfDepth = new ArrayList<>();
            List<CountDownLatch> latchesOfDepth = new ArrayList<>();
            int l = 0;
            for (Stage parent : parentStages) {
                CountDownLatch parentLatch = parentStageLatches.get(l++);
                for (int j = 0; j < width; j++) {
                    Stage stage = new Stage(String.valueOf(stageId.getAndIncrement()), null);
                    parent.addSubStage(stage);
                    stagesOfDepth.add(stage);
                    CountDownLatch currentLatch = new CountDownLatch(1);
                    latchesOfDepth.add(currentLatch);
                    new Thread(() -> {
                        try {
                            parentLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            stage.stageDone();
                        } catch (ProgressBarException e) {
                            e.printStackTrace();
                            Assert.fail();
                        }
                        currentLatch.countDown();
                        mainWait.countDown();
                    }).start();
                }
            }
            parentStages = stagesOfDepth;
            parentStageLatches = latchesOfDepth;
        }
        progressBar.start();
        progressBarCdl.countDown();

        try {
            mainWait.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertTrue(true);
    }
}
