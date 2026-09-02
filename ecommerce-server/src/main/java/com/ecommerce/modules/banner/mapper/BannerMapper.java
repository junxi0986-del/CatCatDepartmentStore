package com.ecommerce.modules.banner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.modules.banner.entity.Banner;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BannerMapper extends BaseMapper<Banner> {
    
    @Select("SELECT * FROM banner WHERE status = 1 ORDER BY sort_order ASC")
    java.util.List<Banner> selectActiveBanners();
}
