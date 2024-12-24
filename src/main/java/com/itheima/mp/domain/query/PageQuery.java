package com.itheima.mp.domain.query;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.mp.domain.po.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "分页查询实体")
public class PageQuery {
    @ApiModelProperty("页码")
    private Integer pageNo = 1;
    @ApiModelProperty("页数")
    private Integer pageSize = 5;
    @ApiModelProperty("排序字段")
    private String sortBy;
    @ApiModelProperty("是否升序")
    private Boolean isAsc = true;

    private <T> Page<T> toMpPage(OrderItem... items) {
        Page<T> page = Page.of(pageNo, pageSize);
        if(StrUtil.isNotBlank(sortBy)){
            page.addOrder(isAsc ? OrderItem.asc(sortBy) : OrderItem.desc(sortBy));
        }else if(items != null){
            page.addOrder(items);
        }
        return page;
    }

    public <T> Page<T> toMpPageDefaultSortByCreateTime() {
        return toMpPage(isAsc ? OrderItem.asc("create_time") : OrderItem.desc("create_time"));
    }

    public <T> Page<T> toMpPageDefaultSortByUpdateTime() {
        return toMpPage(isAsc ? OrderItem.asc("update_time") : OrderItem.desc("update_time"));
    }

    public <T> Page<T> toMpPage(String defaultSortBy, Boolean isAsc) {
        return toMpPage(isAsc ? OrderItem.asc(defaultSortBy) : OrderItem.desc(defaultSortBy));
    }
}
