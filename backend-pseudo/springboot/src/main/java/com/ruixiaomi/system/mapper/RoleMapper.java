package com.ruixiaomi.system.mapper;

import com.ruixiaomi.system.model.RoleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper {

  List<RoleEntity> selectRoleList();

  int insertRole(RoleEntity role);
}