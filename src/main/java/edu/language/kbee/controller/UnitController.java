package edu.language.kbee.controller;

import edu.language.kbee.payload.AdminUnitDto;
import edu.language.kbee.payload.UserUnitDto;
import edu.language.kbee.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UnitController {

    private final UnitService unitService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/courses/{courseId}/units")
    public ResponseEntity<List<AdminUnitDto>> getUnitsForAdmin(@PathVariable String courseId) {
        return ResponseEntity.ok(unitService.getUnitsForAdmin(courseId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/units")
    public ResponseEntity<AdminUnitDto> createUnit(@Valid @RequestBody AdminUnitDto unitDto) {
        AdminUnitDto createdUnit = unitService.createUnit(unitDto);
        return new ResponseEntity<>(createdUnit, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/units/{unitId}/title")
    public ResponseEntity<AdminUnitDto> updateUnitTitle(@PathVariable String unitId,
                                                        @RequestBody String newTitle) {
        return ResponseEntity.ok(unitService.updateUnitTitle(unitId, newTitle));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/units/{unitId}")
    public ResponseEntity<String> deleteUnit(@PathVariable String unitId) {
        unitService.deleteUnit(unitId);
        return ResponseEntity.ok("Unit deleted successfully!");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/courses/{courseId}/units/reorder")
    public ResponseEntity<String> reorderUnits(@PathVariable String courseId,
                                               @RequestBody List<String> unitIds) {
        unitService.reorderUnits(courseId, unitIds);
        return ResponseEntity.ok("Units reordered successfully!");
    }

    @GetMapping("/my-courses/{courseId}/units")
    public ResponseEntity<?> getUnitsForUser(@PathVariable(name = "courseId") String courseId) {

        List<UserUnitDto> unitDtos = unitService.getUnitsForUser(courseId);
        return ResponseEntity.ok(unitDtos);
    }
}
