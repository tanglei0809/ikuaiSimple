package com.ikuai.service;

import com.ikuai.entity.IkuaiParam;
import com.ikuai.entity.UpdateParam;

import java.util.List;

public interface RouterService {

    Object submitDynamicForm(IkuaiParam param);

    Object getDstNatList(IkuaiParam param);

    Object updateDstNatList(List<UpdateParam> params);
}
