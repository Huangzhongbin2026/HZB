package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.dto.SysDictItemDTO;
import com.ruijie.supplysystem.dto.SysDictTypeDTO;
import com.ruijie.supplysystem.service.DictService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DictServiceImpl implements DictService {

    @Override
    public List<SysDictTypeDTO> listTypes() {
        List<SysDictTypeDTO> list = new ArrayList<>();
        SysDictTypeDTO dto = new SysDictTypeDTO();
        dto.setId("1");
        dto.setDictName("任务状态");
        dto.setDictCode("TASK_STATUS");
        dto.setSortNo(1);
        dto.setIsEnabled(true);
        list.add(dto);
        return list;
    }

    @Override
    public Boolean saveType(SysDictTypeDTO dto) { return Boolean.TRUE; }

    @Override
    public List<SysDictItemDTO> listItems(String typeCode, String keyword) {
        List<SysDictItemDTO> list = new ArrayList<>();
        SysDictItemDTO dto = new SysDictItemDTO();
        dto.setId("1");
        dto.setItemName("待处理");
        dto.setItemCode("PENDING");
        dto.setItemValue("1");
        dto.setSortNo(1);
        dto.setIsEnabled(true);
        list.add(dto);
        return list;
    }

    @Override
    public Boolean saveItem(SysDictItemDTO dto) { return Boolean.TRUE; }
}
