package edu.language.kbee.service;

import edu.language.kbee.payload.AdminUnitDto;
import edu.language.kbee.payload.UserUnitDto;

import java.util.List;

public interface UnitService {

    AdminUnitDto createUnit(AdminUnitDto unitDto);
    List<AdminUnitDto> getUnitsForAdmin(String courseId);
    AdminUnitDto updateUnitTitle(String unitId, String newTitle);
    void deleteUnit(String unitId);
    void reorderUnits(String courseId, List<String> unitIdsInNewOrder);
    List<UserUnitDto> getUnitsForUser(String courseId);
}
