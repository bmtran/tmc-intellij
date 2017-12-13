package fi.helsinki.cs.tmc.intellij.services;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;
import fi.helsinki.cs.tmc.intellij.holders.TmcCoreHolder;
import fi.helsinki.cs.tmc.intellij.holders.TmcSettingsManager;
import fi.helsinki.cs.tmc.intellij.services.errors.ErrorMessageService;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Finds various exercises and courses from the disk or by asking the TMCServer. */
public class ObjectFinder {

    private static final Logger logger = LoggerFactory.getLogger(ObjectFinder.class);

    public Exercise findExerciseByName(Course course, String exerciseName) {
        logger.info("Processing findExerciseByName {}. @ObjectFinder", exerciseName);
        List<Exercise> exercises = course.getExercises();
        Exercise exercise = getExercise(exerciseName, exercises);
        if (exercise != null) {
            return exercise;
        }
        logger.info("Could not find exercise with the name {}. @ObjectFinder", exerciseName);
        return null;
    }

    @Nullable
    private Exercise getExercise(String exerciseName, List<Exercise> exercises) {
        for (Exercise exercise : exercises) {
            if (exercise.getName().equals(exerciseName)) {
                logger.info("Found {}. @ObjectFinder", exercise);
                return exercise;
            }
        }
        return null;
    }

    @Nullable
    public Course findCourse(String searchTerm, String titleOrName) {
        TmcCore core = TmcCoreHolder.get();
        List<Course> courses = getCourses(core);

        for (Course c : courses) {
            if ((titleOrName.equals("name") && c.getName().equals(searchTerm)) || (titleOrName.equals("title") && c.getTitle().equals(searchTerm))) {
                try {
                    logger.info(
                            "Trying to get course details from TmcCore. @ObjectFinder", c);

                    return core.getCourseDetails(ProgressObserver.NULL_OBSERVER, c).call();
                } catch (TmcCoreException exception) {
                    logger.warn(
                            "Could not find course. @ObjectFinder",
                            exception,
                            exception.getStackTrace());
                    new ErrorMessageService().showHumanReadableErrorMessage(exception, false);
                } catch (Exception e) {
                    logger.warn(
                            "Could not find course. @ObjectFinder",
                            e,
                            e.getStackTrace());
                    new ErrorMessageService().showErrorMessage(e, "Could not find course.", true);
                }
            }
        }

        return null;
    }

    public List<Course> getCourses(TmcCore core) {
        logger.info("Processing getCourses @ObjectFinder");
        List<Course> courses = null;

        try {
            courses = core.listCourses(ProgressObserver.NULL_OBSERVER).call();
        } catch (TmcCoreException e) {
            logger.warn("Getting courses failed @ObjectFinder", e, e.getStackTrace());
            new ErrorMessageService().showHumanReadableErrorMessage(e, false);
        } catch (Exception e) {
            logger.warn("Getting courses failed @ObjectFinder", e, e.getStackTrace());
            new ErrorMessageService()
                    .showErrorMessage(e, "Something went wrong while trying to get the course list", true);
        }

        return courses;
    }

    public List<String> listAllDownloadedCourses() {
        logger.info("Processing listAllDownloadedCourses. @ObjectFinder");
        List<String> courseTitles = new ArrayList<>();

        for (String name : getListOfDirectoriesInPath(TmcSettingsManager.get().getProjectBasePath())) {
            Course course = findCourse(name, "name");

            if (course == null) {
                continue;
            }

            courseTitles.add(course.getTitle());
        }

        return courseTitles;
    }

    public List<String> listAllDownloadedExercises(String courseTitle) {
        logger.info(
                "Processing listAllDownloadedExercises from course {}. @ObjectFinder", courseTitle);
        return getListOfDirectoriesInPath(
                TmcSettingsManager.get().getProjectBasePath() + File.separator + findCourse(courseTitle, "title").getName());
    }

    private List<String> getListOfDirectoriesInPath(String folderPath) {
        logger.info("Processing addExercisesToList. @ObjectFinder");
        List<String> fileNames = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream =
                Files.newDirectoryStream(Paths.get(folderPath))) {
            logger.info("Getting list of directories in path. @ObjectFinder");
            addExercisesToList(fileNames, directoryStream);
        } catch (Exception ex) {
            logger.warn(
                    "Could not get list of directories in path. @ObjectFinder",
                    ex,
                    ex.getStackTrace());
            ex.printStackTrace();
        }
        Collections.sort(fileNames);
        return fileNames;
    }

    private void addExercisesToList(List<String> fileNames, DirectoryStream<Path> directoryStream) {
        for (Path path : directoryStream) {
            if (!Files.isDirectory(path)) {
                continue;
            }
            String[] exerciseCourse = PathResolver.getCourseAndExerciseName(path);
            if (exerciseCourse == null || getExerciseName(exerciseCourse).charAt(0) == '.') {
                logger.info("exerciseCourse variable = null. @ObjectFinder");
                continue;
            }
            logger.info("Adding exercise to list. @ObjectFinder", getExerciseName(exerciseCourse));
            fileNames.add(getExerciseName(exerciseCourse));
        }
    }

    private String getExerciseName(String[] courseAndExerciseName) {
        logger.info("Trying to getExerciseName. @ObjectFinder");
        return courseAndExerciseName[courseAndExerciseName.length - 1];
    }

    public Project findCurrentProject() {
        logger.info("Trying to findCurrentProject. @ObjectFinder");
        DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
        if (dataContext == null) {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();

            if (projects.length > 0) {
                return projects[projects.length - 1];
            }
            return null;
        }
        return DataKeys.PROJECT.getData(dataContext);
    }

    public Course findCourseNoDetails(String courseName, TmcCore core) {
        try {
            List<Course> list = core.listCourses(ProgressObserver.NULL_OBSERVER).call();
            for (Course cor : list) {
                if (cor.getName().equals(courseName)) {
                    return cor;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}
