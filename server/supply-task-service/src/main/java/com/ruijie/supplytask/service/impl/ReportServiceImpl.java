package com.ruijie.supplytask.service.impl;

import com.ruijie.supplytask.dto.DashboardVO;
import com.ruijie.supplytask.service.ReportService;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {

    @Override
    public DashboardVO getDashboard() {
        return new DashboardVO(156, 23, 8, 93.7);
    }
}
