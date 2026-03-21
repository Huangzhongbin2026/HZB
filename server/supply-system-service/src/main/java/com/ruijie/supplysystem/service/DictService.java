package com.ruijie.supplysystem.service;

import com.ruijie.supplysystem.dto.SysDictItemDTO;
import com.ruijie.supplysystem.dto.SysDictTypeDTO;

import java.util.List;

public interface DictService {
    List<SysDictTypeDTO> listTypes();
    Boolean saveType(SysDictTypeDTO dto);
    List<SysDictItemDTO> listItems(String typeCode, String keyword);
    Boolean saveItem(SysDictItemDTO dto);
}
