package edu.language.kbee.service.impl;


import edu.language.kbee.enums.LessonStatus;
import edu.language.kbee.enums.ProgressStatus;
import edu.language.kbee.model.*;
import edu.language.kbee.payload.LessonDto;
import edu.language.kbee.payload.AdminUnitDto;
import edu.language.kbee.payload.LessonProgressDto;
import edu.language.kbee.payload.UserUnitDto;
import edu.language.kbee.repository.CourseRepository;
import edu.language.kbee.repository.LessonProgressRepository;
import edu.language.kbee.repository.LessonRepository;
import edu.language.kbee.repository.UnitRepository;
import edu.language.kbee.service.UnitService;
import edu.language.kbee.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final AuthUtil authUtil;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public AdminUnitDto createUnit(AdminUnitDto dto) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        int nextOrderIndex = unitRepository.countByCourse_CourseId(course.getCourseId());

        Unit unit = Unit.builder()
                .title(dto.getTitle())
                .orderIndex(nextOrderIndex)
                .course(course)
                .build();

        Unit savedUnit = unitRepository.save(unit);
        return mapToDto(savedUnit);
    }

    @Override
    public List<AdminUnitDto> getUnitsForAdmin(String courseId) {
        List<Unit> units = unitRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId);

        List<AdminUnitDto> unitDtos = units.stream()
                .map(this::mapToDto)
                .toList();

        return unitDtos.stream().map(unitDto -> {

            unitDto.getLessons().sort(Comparator.comparingInt(LessonDto::getOrderIndex));

            return unitDto;
        }).toList();
    }

    @Override
    public List<UserUnitDto> getUnitsForUser(String courseId) {

        User user = authUtil.getLoggedInUser();

        List<Unit> units = unitRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId);
        List<Lesson> lessons = lessonRepository.findByUnit_Course_CourseIdOrderByOrderIndexAsc(courseId);
        List<LessonProgress> progresses = progressRepository.findAllByUserIdAndCourseId(user.getUserId(), courseId);

        Map<String, LessonProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(p -> p.getLesson().getLessonId(), p -> p));

        Map<String, List<LessonProgressDto>> lessonsByUnit = lessons.stream()
                .filter(lesson -> lesson.getStatus() == LessonStatus.PUBLISHED)
                .map(lesson -> {
                    LessonProgress prog = progressMap.get(lesson.getLessonId());
                    return LessonProgressDto.builder()
                            .lessonId(lesson.getLessonId())
                            .unitId(lesson.getUnit().getUnitId())
                            .title(lesson.getTitle())
                            .type(lesson.getType())
                            .durationMinutes(lesson.getDurationMinutes())
                            .orderIndex(lesson.getOrderIndex())
                            .status(prog != null ? prog.getStatus() : ProgressStatus.NOT_STARTED)
                            .build();
                })
                .collect(Collectors.groupingBy(LessonProgressDto::getUnitId));


        List<UserUnitDto> result = new ArrayList<>();

        boolean canUnlockNext = true;

        for (Unit u : units) {
            UserUnitDto temp = modelMapper.map(u, UserUnitDto.class);
            temp.setCourseId(courseId);

            List<LessonProgressDto> unitLessons = lessonsByUnit.getOrDefault(u.getUnitId(), new ArrayList<>())
                    .stream()
                    .sorted(Comparator.comparingInt(LessonProgressDto::getOrderIndex))
                    .collect(Collectors.toList());

            for (LessonProgressDto lesson : unitLessons) {
                if (!canUnlockNext) {
                    lesson.setStatus(ProgressStatus.LOCKED);
                } else {
                    if (lesson.getStatus() != ProgressStatus.COMPLETED) {
                        canUnlockNext = false;
                    }
                }
            }

            ProgressStatus unitStatus = getUnitProgressStatus(unitLessons);
            temp.setProgressStatus(unitStatus);
            temp.setLessons(unitLessons);

            result.add(temp);
        }

        return result;
    }

    private ProgressStatus getUnitProgressStatus(List<LessonProgressDto> unitLessons) {
        if (unitLessons == null || unitLessons.isEmpty()) {
            return ProgressStatus.NOT_STARTED;
        }

        boolean isAllLocked = unitLessons.stream()
                .allMatch(lesson -> lesson.getStatus() == ProgressStatus.LOCKED);
        if (isAllLocked) {
            return ProgressStatus.LOCKED;
        }

        boolean isAllCompleted = unitLessons.stream()
                .allMatch(lesson -> lesson.getStatus() == ProgressStatus.COMPLETED);
        if (isAllCompleted) {
            return ProgressStatus.COMPLETED;
        }

        boolean isAnyInProgressOrCompleted = unitLessons.stream()
                .anyMatch(lesson -> lesson.getStatus() == ProgressStatus.IN_PROGRESS
                        || lesson.getStatus() == ProgressStatus.COMPLETED);
        if (isAnyInProgressOrCompleted) {
            return ProgressStatus.IN_PROGRESS;
        }

        return ProgressStatus.NOT_STARTED;
    }

    @Override
    @Transactional
    public AdminUnitDto updateUnitTitle(String unitId, String newTitle) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        unit.setTitle(newTitle);
        Unit updatedUnit = unitRepository.save(unit);
        return modelMapper.map(updatedUnit, AdminUnitDto.class);
    }

    @Override
    @Transactional
    public void deleteUnit(String unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        unitRepository.delete(unit);
    }

    @Override
    @Transactional
    public void reorderUnits(String courseId, List<String> unitIdsInNewOrder) {
        List<Unit> existingUnits = unitRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId);

        Map<String, Unit> unitMap = existingUnits.stream()
                .collect(Collectors.toMap(Unit::getUnitId, Function.identity()));

        for (int i = 0; i < unitIdsInNewOrder.size(); i++) {
            String id = unitIdsInNewOrder.get(i);
            if (unitMap.containsKey(id)) {
                Unit unit = unitMap.get(id);
                unit.setOrderIndex(i);
            }
        }

        unitRepository.saveAll(existingUnits);
    }


    private AdminUnitDto mapToDto(Unit unit) {
        List<LessonDto> lessonDtos = null;
        if (unit.getLessons() != null) {
            lessonDtos = unit.getLessons().stream().map(lesson -> LessonDto.builder()
                    .lessonId(lesson.getLessonId())
                    .title(lesson.getTitle())
                    .type(lesson.getType())
                    .orderIndex(lesson.getOrderIndex())
                    .durationMinutes(lesson.getDurationMinutes())
                    .status(lesson.getStatus())
                    .unitId(unit.getUnitId())
                    .build()
            ).collect(Collectors.toList());
        } else {
            lessonDtos = List.of();
        }

        return AdminUnitDto.builder()
                .unitId(unit.getUnitId())
                .title(unit.getTitle())
                .orderIndex(unit.getOrderIndex())
                .courseId(unit.getCourse().getCourseId())
                .lessons(lessonDtos)
                .build();
    }
}
