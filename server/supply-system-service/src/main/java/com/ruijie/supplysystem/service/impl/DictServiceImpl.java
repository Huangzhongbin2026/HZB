package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.dto.SysDictItemDTO;
import com.ruijie.supplysystem.dto.SysDictTypeDTO;
import com.ruijie.supplysystem.mapper.DictMapper;
import com.ruijie.supplysystem.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictMapper dictMapper;

    @Override
    public List<SysDictTypeDTO> listTypes() {
        return dictMapper.listTypes();
    }

    @Override
    public Boolean saveType(SysDictTypeDTO dto) {
        return dictMapper.insertType(dto) > 0;
    }

    @Override
    public List<SysDictItemDTO> listItems(String typeCode, String keyword) {
        return dictMapper.listItems(typeCode, keyword);
    }

    @Override
    public Boolean saveItem(SysDictItemDTO dto) {
        if (dto.getDictTypeId() == null || dto.getDictTypeId().isBlank()) {
            throw new BusinessException(3101, "字典分类ID不能为空");
        }
        return dictMapper.insertItem(dto) > 0;
    }
}
