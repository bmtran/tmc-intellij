package fi.helsinki.cs.tmc.intellij.actions;

import com.google.common.base.Optional;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.utilities.TmcServerAddressNormalizer;
import fi.helsinki.cs.tmc.intellij.holders.ExerciseDatabaseManager;
import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.io.CoreProgressObserver;
import fi.helsinki.cs.tmc.intellij.io.SettingsTmc;
import fi.helsinki.cs.tmc.intellij.services.ProgressWindowMaker;
import fi.helsinki.cs.tmc.intellij.services.ThreadingService;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;
import fi.helsinki.cs.tmc.intellij.services.exercises.CheckForNewExercises;
import fi.helsinki.cs.tmc.intellij.services.exercises.CourseAndExerciseManager;
import fi.helsinki.cs.tmc.intellij.services.logging.PropertySetter;
import fi.helsinki.cs.tmc.intellij.snapshots.ActivateSnapshotsListeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindowManager;

import fi.helsinki.cs.tmc.intellij.ui.login.LoginDialog;
import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The actions to be executed on project startup defined in plugin.xml exercises group on line
 * &lt;postStartupActivity implementation ="fi.helsinki.cs.tmc.intellij.actions.StartupEvent"&gt;
 */
public class StartupEvent implements StartupActivity {

    private static final Logger logger = LoggerFactory.getLogger(StartupEvent.class);

    @Override
    public void runActivity(@NotNull Project project) {

        logger.info("Opening project {} and running startup actions. @StartupEvent", project);

        ExerciseDatabaseManager.setup();

        ThreadingService threadingService = new ThreadingService();

        ProgressWindow progressWindow =
                ProgressWindowMaker.make(
                        "Running TMC startup actions.", project, false, false, false);

        CoreProgressObserver observer = new CoreProgressObserver(progressWindow);
        new ErrorMessageService()
                .showInfoBalloon(
                        "The Test My Code Plugin for Intellij is in BETA and"
                                + " may not work properly. Use at your own risk. ");

        threadingService.runWithNotification(
                new Thread(
                        () -> {
                            setupLoggers(observer);
                            setupTmcSettings(observer);
                            CheckForOneDrive.run();
                            setupCoreHolder(observer);
                            setupSnapshots(observer, project);
                            setupDatabase(observer);
                            setupHandlersForSnapshots(observer);

                            if (TmcSettingsManager.get().getFirstRun()) {
                                TmcSettingsManager.get().setFirstRun(false);
                            } else {
                                sendDiagnostics(observer);
                            }

                            checkForNewExercises(observer);

                            ApplicationManager.getApplication()
                                    .invokeLater(
                                            () -> {
                                                while (project.isDisposed()) {
                                                    try {
                                                        Thread.sleep(5);
                                                    } catch (Exception e) {

                                                    }
                                                }

                                                if (ToolWindowManager.getInstance(project)
                                                                .getToolWindow("Project")
                                                        != null) {
                                                    ToolWindowManager.getInstance(project)
                                                            .getToolWindow("Project")
                                                            .activate(null);
                                                }
                                            });
                        }),
                project,
                progressWindow);

        showLoginWindow();
    }

    private void setupLoggers(ProgressObserver observer) {
        observer.progress(0, 0.0, "Initializing loggers");
        PropertySetter propSet = new PropertySetter();
        propSet.setLog4jProperties();
    }

    private void setupTmcSettings(ProgressObserver observer) {
        observer.progress(0, 0.14, "Loading settings");
        TmcSettingsManager.setup();
    }

    private void setupCoreHolder(ProgressObserver observer) {
        observer.progress(0, 0.28, "Holding core");
        TmcCoreHolder.setup();
    }

    private void setupSnapshots(ProgressObserver observer, Project project) {
        observer.progress(0, 0.42, "Activating listeners");
        new ActivateSnapshotsListeners(project).activateListeners();
    }

    private void setupDatabase(ProgressObserver observer) {
        if (TmcSettingsManager.get().getToken().isPresent()) {
            observer.progress(0, 0.56, "Initializing database");
            new CourseAndExerciseManager().initiateDatabase();
        }
    }

    private void setupHandlersForSnapshots(ProgressObserver observer) {
        observer.progress(0, 0.70, "Setting handlers");
        final EditorActionManager actionManager = EditorActionManager.getInstance();
        final TypedAction typedAction = actionManager.getTypedAction();
        TypedActionHandler originalHandler = actionManager.getTypedAction().getHandler();
        typedAction.setupHandler(new ActivateSnapshotsAction(originalHandler));
    }

    private void checkForNewExercises(ProgressObserver observer) {
        observer.progress(0, 0.84, "Checking for new exercises");
        if (TmcSettingsManager.get().isCheckForExercises()) {
            new CheckForNewExercises().doCheck();
        }
    }

    private void sendDiagnostics(ProgressObserver observer) {
        if (TmcSettingsManager.get().getSendDiagnostics()) {
            try {
                TmcCoreHolder.get().sendDiagnostics(observer).call();
            } catch (Exception e) {
            }
        }
    }

    private void tryToMigratePasswordToOAuthToken() {
        final SettingsTmc settings = TmcSettingsManager.get();
        try {
            TmcServerAddressNormalizer normalizer = new TmcServerAddressNormalizer();
            normalizer.normalize();
            TmcCoreHolder.get()
                    .authenticate(ProgressObserver.NULL_OBSERVER, settings.getPassword().get())
                    .call();
            normalizer.selectOrganizationAndCourse();

            this.migrateCourseAndExerciseDatabase();
        } catch (Exception ex) {
            logger.info(
                    "Couldn't migrate password to OAuth token. The user will be asked to log in.");
            settings.setToken(Optional.absent());
        } finally {
            settings.setPassword(Optional.absent());
        }
    }

    private void migrateCourseAndExerciseDatabase() {
        new CourseAndExerciseManager().initiateDatabase();
    }

    private void showLoginWindow() {
        SettingsTmc settingsTmc = TmcSettingsManager.get();
        if (settingsTmc.getPassword().isPresent()) {
            this.tryToMigratePasswordToOAuthToken();
        }
        if (!settingsTmc.getToken().isPresent() || settingsTmc.getServerAddress().isEmpty()) {
            LoginDialog.display();
        }
    }
}
