package org.jeecg.modules.management.wxuserregister.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.management.wxuserregister.entity.WxUserRegister;
import org.jeecg.modules.management.wxuserregister.service.IWxUserRegisterService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;

 /**
 * @Description: 微信用户注册
 * @Author: jeecg-boot
 * @Date:   2020-04-29
 * @Version: V1.0
 */
@RestController
@RequestMapping("/wxuserregister/wxUserRegister")
@Slf4j
public class WxUserRegisterController extends JeecgController<WxUserRegister, IWxUserRegisterService> {
	@Autowired
	private IWxUserRegisterService wxUserRegisterService;
	
	/**
	 * 分页列表查询
	 *
	 * @param wxUserRegister
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<?> queryPageList(WxUserRegister wxUserRegister,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
	    if (StringUtils.isNotBlank(wxUserRegister.getCompanyName())) {
	        wxUserRegister.setCompanyName("*"+wxUserRegister.getCompanyName().trim()+"*");
        }
        if (StringUtils.isNotBlank(wxUserRegister.getNickname())) {
            wxUserRegister.setNickname("*"+wxUserRegister.getNickname().trim()+"*");
        }
		QueryWrapper<WxUserRegister> queryWrapper = QueryGenerator.initQueryWrapper(wxUserRegister, req.getParameterMap());
		Page<WxUserRegister> page = new Page<WxUserRegister>(pageNo, pageSize);
		IPage<WxUserRegister> pageList = wxUserRegisterService.page(page, queryWrapper);
		return Result.ok(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param wxUserRegister
	 * @return
	 */
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody WxUserRegister wxUserRegister) {
		wxUserRegisterService.save(wxUserRegister);
		return Result.ok("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param wxUserRegister
	 * @return
	 */
	@PutMapping(value = "/edit")
	public Result<?> edit(@RequestBody WxUserRegister wxUserRegister) {
		wxUserRegisterService.updateById(wxUserRegister);
		return Result.ok("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		wxUserRegisterService.removeById(id);
		return Result.ok("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.wxUserRegisterService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.ok("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		WxUserRegister wxUserRegister = wxUserRegisterService.getById(id);
		if(wxUserRegister==null) {
			return Result.error("未找到对应数据");
		}
		return Result.ok(wxUserRegister);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param wxUserRegister
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WxUserRegister wxUserRegister) {
        return super.exportXls(request, wxUserRegister, WxUserRegister.class, "微信用户注册");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WxUserRegister.class);
    }

}
