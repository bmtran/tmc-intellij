package fi.helsinki.cs.tmc.intellij.actions;

import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.services.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.services.ErrorMessageService;
import fi.helsinki.cs.tmc.intellij.services.ObjectFinder;
import fi.helsinki.cs.tmc.intellij.services.PathResolver;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.ui.testresults.TestResultPanelFactory;
import fi.helsinki.cs.tmc.langs.domain.RunResult;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;

public class RunTestsAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        String[] courseExercise = PathResolver.getCourseAndExerciseName(anActionEvent.getProject());

        CourseAndExerciseManager courseAndExerciseManager = new CourseAndExerciseManager();

        runTests(courseAndExerciseManager.getExercise(getCourseName(courseExercise),
                getExerciseName(courseExercise)), anActionEvent.getProject());
    }


    public void runTests(final Exercise exercise, Project project) {
        if (exercise != null) {
            ThreadingService.runWithNotification(new Runnable() {
                @Override
                public void run() {
                    RunResult result = null;
                    try {
                        result = TmcCoreHolder.get()
                                .runTests(ProgressObserver.NULL_OBSERVER, exercise).call();
                        RunResult finalResult = result;
                        showTestResult(finalResult);
                    } catch (Exception e) {
                        ErrorMessageService error = new ErrorMessageService();
                        error.showMessage(e, "Running tests failed!", true);
                    }
                }
            }, "Running tests!", project);
            displayTestWindow();
        } else {
            ErrorMessageService error = new ErrorMessageService();
            error.showMessage(new Exception(),
                    "Running tests failed, exercise was not recognized",true);
        }
    }

    public static void displayTestWindow() {
        ToolWindowManager.getInstance(new ObjectFinder().findCurrentProject())
                .getToolWindow("TMC Test Results").show(null);
        ToolWindowManager.getInstance(new ObjectFinder().findCurrentProject())
                .getToolWindow("TMC Test Results").activate(null);
    }

    public static void showTestResult(final RunResult finalResult) {
        ApplicationManager.getApplication().invokeLater((new Runnable() {
            @Override
            public void run() {
                TestResultPanelFactory.updateMostRecentResult(finalResult.testResults);
            }
        }));
    }

    private String getCourseName(String[] courseExercise) {
        return courseExercise[courseExercise.length - 2];
    }

    private String getExerciseName(String[] courseExercise) {
        return courseExercise[courseExercise.length - 1];
    }
}
