package org.jeecg.modules.management.client.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.HttpHelper;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.management.amap.constant.AMapConstant;
import org.jeecg.modules.management.client.entity.Client;
import org.jeecg.modules.management.client.entity.Contact;
import org.jeecg.modules.management.client.entity.DeviceNumber;
import org.jeecg.modules.management.client.entity.WxUser;
import org.jeecg.modules.management.client.service.IClientService;
import org.jeecg.modules.management.client.service.IContactService;
import org.jeecg.modules.management.client.service.IDeviceNumberService;
import org.jeecg.modules.management.client.service.IWxUserService;
import org.jeecg.modules.management.erp.erpinterface.ERPInterfaceConstant;
import org.jeecg.modules.management.erp.erptoken.util.ERPTokenUtils;
import org.jeecg.modules.management.workorder.entity.WorkOrder;
import org.jeecg.modules.management.workorder.service.IWorkOrderService;
import org.jeecg.modules.management.wxuserregister.entity.WxUserRegister;
import org.jeecg.modules.management.wxuserregister.service.IWxUserRegisterService;
import org.jeecg.modules.wechat.constant.WechatConstant;
import org.jeecg.modules.wechat.entity.WxUserEntity;
import org.jeecg.modules.wechat.util.WxUserUtils;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @Description: 客户信息
 * @Author: jeecg-boot
 * @Date:   2020-03-10
 * @Version: V1.0
 */
@RestController
@RequestMapping("/client/client")
@Slf4j
public class ClientController extends JeecgController<Client, IClientService> {

	@Autowired
	private IClientService clientService;

	@Autowired
	private IContactService contactService;

	@Autowired
	private IDeviceNumberService deviceNumberService;

     @Autowired
     private IWxUserService wxUserService;

     @Autowired
     private RedisUtil redisUtil;

     @Autowired
     private IWorkOrderService workOrderService;

    @Autowired
    private IWxUserRegisterService wxUserRegisterService;


	/*---------------------------------主表处理-begin-------------------------------------*/

	/**
	 * 分页列表查询
	 * @param client
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<?> queryPageList(Client client,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
	    if (StringUtils.isNotBlank(client.getNumber())) {
	        client.setNumber("*"+client.getNumber().trim()+"*");
        }
        if (StringUtils.isNotBlank(client.getName())) {
            client.setName("*"+client.getName().trim()+"*");
        }
		QueryWrapper<Client> queryWrapper = QueryGenerator.initQueryWrapper(client, req.getParameterMap());
		Page<Client> page = new Page<Client>(pageNo, pageSize);
		IPage<Client> pageList = clientService.page(page, queryWrapper);
		return Result.ok(pageList);
	}

	/**
     *   添加
     * @param client
     * @return
     */
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody Client client) {
        clientService.save(client);
        return Result.ok("添加成功！");
    }

    /**
     *  编辑
     * @param client
     * @return
     */
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody Client client) {
        clientService.updateById(client);
        return Result.ok("编辑成功!");
    }

    /**
     * 通过id删除
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name="id",required=true) String id) {
        clientService.delMain(id);
        return Result.ok("删除成功!");
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
        this.clientService.delBatchMain(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功!");
    }

    /**
     * 导出
     * @return
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Client client) {
        return super.exportXls(request, client, Client.class, "客户信息");
    }

    /**
     * 导入
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, Client.class);
    }

     /**
      * 同步客户
      * @return
      */
    @RequestMapping(value = "/synchronizeClient", method = RequestMethod.GET)
    public Result<?> synchronizeClient() {
        QueryWrapper<Client> clientQueryWrapper = new QueryWrapper<>();
        clientQueryWrapper.orderByDesc("modifytime");
        List<Client> clientList = clientService.list(clientQueryWrapper);
        try {
            ERPTokenUtils.setToken();
            JSONObject jsonObject;
            if (clientList.isEmpty()) {
                jsonObject = HttpHelper.httpGet(ERPInterfaceConstant.API_DOMAIN_NAME+ERPInterfaceConstant.LIST_CLIENT_URL.replace("TOKEN",redisUtil.get(ERPInterfaceConstant.TOKEN_KEY).toString()).replace("MAX",""));
            } else {
                jsonObject = HttpHelper.httpGet(ERPInterfaceConstant.API_DOMAIN_NAME+ERPInterfaceConstant.LIST_CLIENT_URL.replace("TOKEN",redisUtil.get(ERPInterfaceConstant.TOKEN_KEY).toString()).replace("MAX",clientList.get(0).getModifytime().getTime()+""));
            }
            JSONArray dataArray = jsonObject.getJSONArray("data");
            DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject data = dataArray.getJSONObject(i);
                QueryWrapper<Client> checkQueryWrapper = new QueryWrapper<>();
                checkQueryWrapper.eq("number",data.getString("FNumber"));
                Client editClient = clientService.getOne(checkQueryWrapper);
                if (oConvertUtils.isEmpty(editClient)) {
                    Client addClient = new Client();
                    addClient.setNumber(data.getString("FNumber"));
                    addClient.setName(data.getString("FName"));
                    // 设置经纬度
                    setLocation(data, addClient);
                    addClient.setType("1");
                    addClient.setCreateTime(dataFormat.parse(data.getString("FRegDate")));
                    addClient.setModifytime(data.getTimestamp("FModifyTime"));
                    clientService.save(addClient);
                    if (oConvertUtils.isNotEmpty(data.get("FContact"))) {
                        Contact contact = new Contact();
                        contact.setName(data.getString("FContact").trim());
                        if (oConvertUtils.isNotEmpty(data.get("FMobilePhone"))) {
                            contact.setMobilePhone(data.getString("FMobilePhone"));
                        }
                        contact.setClientId(addClient.getId());
                        contactService.save(contact);
                    }
                } else {
                    editClient.setName(data.getString("FName"));
                    // 设置经纬度
                    setLocation(data, editClient);
                    if (oConvertUtils.isNotEmpty(data.get("FContact"))) {
                        QueryWrapper<Contact> contactQueryWrapper = new QueryWrapper<>();
                        contactQueryWrapper.eq("client_id",editClient.getId());
                        contactQueryWrapper.eq("name",data.getString("FContact").trim());
                        Contact editContact = contactService.getOne(contactQueryWrapper);
                        if (oConvertUtils.isEmpty(editContact)) {
                            Contact addContact = new Contact();
                            addContact.setName(data.getString("FContact").trim());
                            if (oConvertUtils.isNotEmpty(data.get("FMobilePhone"))) {
                                addContact.setMobilePhone(data.getString("FMobilePhone"));
                            }
                            addContact.setClientId(editClient.getId());
                            contactService.save(addContact);
                        } else {
                            if (oConvertUtils.isNotEmpty(data.get("FMobilePhone")) && !editContact.getMobilePhone().equals(data.getString("FMobilePhone"))) {
                                editContact.setMobilePhone(data.getString("FMobilePhone"));
                                contactService.updateById(editContact);
                            }
                        }
                    }
                    editClient.setCreateTime(dataFormat.parse(data.getString("FRegDate")));
                    editClient.setModifytime(data.getTimestamp("FModifyTime"));
                    clientService.updateById(editClient);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("同步客户出错!");
            return Result.error("同步出错,请联系管理员!");
        }
        return Result.ok("同步成功!");
    }

    /**
     * 设置经纬度
     * @param data
     * @param client
     */
    private void setLocation(JSONObject data, Client client) {
        if (oConvertUtils.isNotEmpty(data.get("FAddress"))) {
            client.setAddress(data.getString("FAddress").replaceAll(" ",""));
            JSONObject amapResult = HttpHelper.httpGet(AMapConstant.GET_LOCATION_BY_ADDRESS_URL.replace("ADDRESS",data.getString("FAddress").replaceAll(" ","")));
            if (Integer.parseInt(amapResult.getString("count")) > 0) {
                JSONArray geocodeArray = amapResult.getJSONArray("geocodes");
                client.setLongitude(Double.parseDouble(geocodeArray.getJSONObject(0).getString("location").split(",")[0]));
                client.setLatitude(Double.parseDouble(geocodeArray.getJSONObject(0).getString("location").split(",")[1]));
            }
        }
    }

    /**
      * 获取工单客户
      * @return
      */
    @GetMapping(value = "/listPageByWorkOrder")
    public Result<?> listPageByWorkOrder(Client client,@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,@RequestParam(name="pageSize", defaultValue="10") Integer pageSize,HttpServletRequest req) {
        List<WorkOrder> workOrderList = workOrderService.list();
        List<String> clientIdsList = new ArrayList<>();
        for (WorkOrder workOrder : workOrderList) {
            clientIdsList.add(workOrder.getClientId());
        }
        if (clientIdsList.isEmpty()) {
            return Result.error("暂无客户数据!");
        }
        QueryWrapper<Client> clientQueryWrapper = QueryGenerator.initQueryWrapper(client, req.getParameterMap());
        clientQueryWrapper.in("id",clientIdsList);
        Page<Client> page = new Page<>(pageNo,pageSize);
        IPage<Client> pageList = clientService.page(page, clientQueryWrapper);
        return Result.ok(pageList);
    }

    /**
     * 根据id获取客户信息
     * @param id
     * @return
     */
    @GetMapping(value = "/getClientById")
    public Result<?> queryClientById(@RequestParam("id") String id) {
        return Result.ok(clientService.getById(id));
    }

	/*---------------------------------主表处理-end-------------------------------------*/
	

    /*--------------------------------子表处理-联系人信息-begin----------------------------------------------*/
	/**
	 * 查询子表信息 会传入主表ID
	 * @return
	 */
	@GetMapping(value = "/listContactByMainId")
    public Result<?> listContactByMainId(Contact contact,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest req) {
        QueryWrapper<Contact> queryWrapper = QueryGenerator.initQueryWrapper(contact, req.getParameterMap());
        Page<Contact> page = new Page<Contact>(pageNo, pageSize);
        IPage<Contact> pageList = contactService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

	/**
	 * 添加
	 * @param contact
	 * @return
	 */
	@PostMapping(value = "/addContact")
	public Result<?> addContact(@RequestBody Contact contact) {
		contactService.save(contact);
		return Result.ok("添加成功！");
	}

    /**
     * 添加
     * @param contact
     * @return
     */
    @PostMapping(value = "/addContactReturn")
    public Result<?> addContactReturn(@RequestBody Contact contact) {
        contactService.save(contact);
        return Result.ok(contact);
    }

    /**
	 * 编辑
	 * @param contact
	 * @return
	 */
	@PutMapping(value = "/editContact")
	public Result<?> editContact(@RequestBody Contact contact) {
		contactService.updateById(contact);
		return Result.ok("编辑成功!");
	}

	/**
	 * 通过id删除
	 * @param id
	 * @return
	 */
	@DeleteMapping(value = "/deleteContact")
	public Result<?> deleteContact(@RequestParam(name="id",required=true) String id) {
		contactService.removeById(id);
		return Result.ok("删除成功!");
	}

	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@DeleteMapping(value = "/deleteBatchContact")
	public Result<?> deleteBatchContact(@RequestParam(name="ids",required=true) String ids) {
		this.contactService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功!");
	}

    /**
     * 根据id获取数据
     * @param id
     * @return
     */
	@GetMapping(value = "/getContactById")
    public Result<?> queryContactById(@RequestParam("id") String id) {
	    return Result.ok(contactService.getById(id));
    }

    /*--------------------------------子表处理-联系人信息-end----------------------------------------------*/

    /*--------------------------------子表处理-设备编号-begin----------------------------------------------*/

	/**
	 * 查询子表信息 会传入主表ID
	 * @return
	 */
	@GetMapping(value = "/listDeviceNumberByMainId")
    public Result<?> listDeviceNumberByMainId(DeviceNumber deviceNumber,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest req) {
	    if (StringUtils.isNotBlank(deviceNumber.getNumber())) {
	        deviceNumber.setNumber(deviceNumber.getNumber().trim());
        }
        if (StringUtils.isNotBlank(deviceNumber.getName())) {
            deviceNumber.setName("*"+deviceNumber.getName().trim()+"*");
        }
        if (StringUtils.isNotBlank(deviceNumber.getType())) {
            deviceNumber.setType(deviceNumber.getType().trim());
        }
        QueryWrapper<DeviceNumber> queryWrapper = QueryGenerator.initQueryWrapper(deviceNumber, req.getParameterMap());
        if (StringUtils.isNotBlank(req.getParameter("clientName"))) {
            QueryWrapper<Client> clientQueryWrapper = new QueryWrapper<>();
            clientQueryWrapper.like("name",req.getParameter("clientName").trim());
            List<Client> clientList = clientService.list(clientQueryWrapper);
            if (!clientList.isEmpty()) {
                List<String> clientIdsList = new ArrayList<>();
                for (Client client : clientList) {
                    clientIdsList.add(client.getId());
                }
                queryWrapper.in("client_id",clientIdsList);
            } else {
                queryWrapper.eq("client_id","0");
            }
        }
        Page<DeviceNumber> page = new Page<DeviceNumber>(pageNo, pageSize);
        IPage<DeviceNumber> pageList = deviceNumberService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

	/**
	 * 添加
	 * @param deviceNumber
	 * @return
	 */
	@PostMapping(value = "/addDeviceNumber")
	public Result<?> addDeviceNumber(@RequestBody DeviceNumber deviceNumber) {
	    deviceNumber.setAddMethod("0");
		deviceNumberService.save(deviceNumber);
		return Result.ok("添加成功！");
	}

    /**
	 * 编辑
	 * @param deviceNumber
	 * @return
	 */
	@PutMapping(value = "/editDeviceNumber")
	public Result<?> editDeviceNumber(@RequestBody DeviceNumber deviceNumber) {
		deviceNumberService.updateById(deviceNumber);
		return Result.ok("编辑成功!");
	}

	/**
	 * 通过id删除
	 * @param id
	 * @return
	 */
	@DeleteMapping(value = "/deleteDeviceNumber")
	public Result<?> deleteDeviceNumber(@RequestParam(name="id",required=true) String id) {
		deviceNumberService.removeById(id);
		return Result.ok("删除成功!");
	}

	/**
	 * 批量删除
	 * @param ids
	 * @return
	 */
	@DeleteMapping(value = "/deleteBatchDeviceNumber")
	public Result<?> deleteBatchDeviceNumber(@RequestParam(name="ids",required=true) String ids) {
		this.deviceNumberService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.ok("批量删除成功!");
	}

     /**
      * 同步设备编号
      * @return
      */
	@RequestMapping(value = "/synchronizeDeviceNumber", method = RequestMethod.GET)
    public Result<?> synchronizeDeviceNumber() {
	    QueryWrapper<DeviceNumber> deviceNumberQueryWrapper = new QueryWrapper<>();
	    deviceNumberQueryWrapper.orderByDesc("modifytime");
	    List<DeviceNumber> deviceNumberList = deviceNumberService.list(deviceNumberQueryWrapper);
	    try {
            JSONObject jsonObject;
            ERPTokenUtils.setToken();
            if (deviceNumberList.isEmpty()) {
                jsonObject = HttpHelper.httpGet(ERPInterfaceConstant.API_DOMAIN_NAME+ERPInterfaceConstant.LIST_DEVICENUMBER_URL.replace("TOKEN",redisUtil.get(ERPInterfaceConstant.TOKEN_KEY).toString()).replace("MAX",""));
            } else {
                jsonObject = HttpHelper.httpGet(ERPInterfaceConstant.API_DOMAIN_NAME+ERPInterfaceConstant.LIST_DEVICENUMBER_URL.replace("TOKEN",redisUtil.get(ERPInterfaceConstant.TOKEN_KEY).toString()).replace("MAX",deviceNumberList.get(0).getModifytime().getTime()+""));
            }
            JSONArray dataArray = jsonObject.getJSONArray("data");
            DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<DeviceNumber> addDeviceNumberList = new ArrayList<>();
            List<DeviceNumber> editDeviceNumberList = new ArrayList<>();
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject data = dataArray.getJSONObject(i);
                QueryWrapper<Client> clientQueryWrapper = new QueryWrapper<>();
                clientQueryWrapper.eq("number",data.getString("custNo"));
                Client client = clientService.getOne(clientQueryWrapper);
                if (oConvertUtils.isNotEmpty(client)) {
                    QueryWrapper<DeviceNumber> checkQueryWrapper = new QueryWrapper<>();
                    checkQueryWrapper.eq("number",data.getString("number"));
                    DeviceNumber editDeviceNumber = deviceNumberService.getOne(checkQueryWrapper);
                    if (oConvertUtils.isEmpty(editDeviceNumber)) {
                        DeviceNumber addDeviceNumber = new DeviceNumber();
                        addDeviceNumber.setNumber(data.getString("number"));
                        addDeviceNumber.setName(data.getString("name"));
                        addDeviceNumber.setType(data.getString("type"));
                        if (oConvertUtils.isNotEmpty(data.get("signing"))) {
                            addDeviceNumber.setSigning(dataFormat.parse(data.getString("signing")));
                        }
                        if (oConvertUtils.isNotEmpty(data.get("describe"))) {
                            addDeviceNumber.setDescription(data.getString("describe"));
                        }
                        if (oConvertUtils.isNotEmpty(data.get("QGP"))) {
                            addDeviceNumber.setQgp(dataFormat.parse(data.getString("QGP")));
                        }
                        if (oConvertUtils.isNotEmpty(data.get("acceptance"))) {
                            addDeviceNumber.setAcceptance(dataFormat.parse(data.getString("acceptance")));
                        }
                        addDeviceNumber.setModifytime(data.getTimestamp("FModifyTime"));
                        addDeviceNumber.setClientId(client.getId());
                        addDeviceNumberList.add(addDeviceNumber);
                    } else {
                        editDeviceNumber.setName(data.getString("name"));
                        editDeviceNumber.setType(data.getString("type"));
                        if (oConvertUtils.isNotEmpty(data.get("signing"))) {
                            editDeviceNumber.setSigning(dataFormat.parse(data.getString("signing")));
                        }
                        if (oConvertUtils.isNotEmpty(data.get("describe"))) {
                            editDeviceNumber.setDescription(data.getString("describe"));
                        }
                        if (oConvertUtils.isNotEmpty(data.get("QGP"))) {
                            editDeviceNumber.setQgp(dataFormat.parse(data.getString("QGP")));
                        }
                        if (oConvertUtils.isNotEmpty(data.get("acceptance"))) {
                            editDeviceNumber.setAcceptance(dataFormat.parse(data.getString("acceptance")));
                        }
                        editDeviceNumber.setModifytime(data.getTimestamp("FModifyTime"));
                        editDeviceNumberList.add(editDeviceNumber);
                    }
                }
            }
            if (!addDeviceNumberList.isEmpty()) {
                deviceNumberService.saveBatch(addDeviceNumberList);
            }
            if (!editDeviceNumberList.isEmpty()) {
                deviceNumberService.updateBatchById(editDeviceNumberList);
            }
        } catch (Exception e) {
	        e.printStackTrace();
	        log.error("同步设备编号出错!");
	        return Result.error("同步出错,请联系管理员!");
        }
	    return Result.ok("同步成功!");
    }

    /**
     * 导出设备档案
     * @return
     */
    @RequestMapping(value = "/exportXlsDeviceNumber")
    public ModelAndView exportXlsDeviceNumber(HttpServletRequest request, DeviceNumber deviceNumber) {
        // Step.1 组装查询条件
        QueryWrapper<DeviceNumber> queryWrapper = QueryGenerator.initQueryWrapper(deviceNumber, request.getParameterMap());
        queryWrapper.eq("add_method","0");
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

        // Step.2 获取导出数据
        List<DeviceNumber> pageList = deviceNumberService.list(queryWrapper);
        List<DeviceNumber> exportList = null;

        // 过滤选中数据
        String selections = request.getParameter("selections");
        if (oConvertUtils.isNotEmpty(selections)) {
            List<String> selectionList = Arrays.asList(selections.split(","));
            exportList = pageList.stream().filter(item -> selectionList.contains(getId(item))).collect(Collectors.toList());
        } else {
            exportList = pageList;
        }

        // Step.3 AutoPoi 导出Excel
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.FILE_NAME, "设备档案"); //此处设置的filename无效 ,前端会重更新设置一下
        mv.addObject(NormalExcelConstants.CLASS, DeviceNumber.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("设备档案", "导出人:" + sysUser.getRealname(), "设备档案"));
        mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        return mv;
    }

    /**
     * 获取对象ID
     *
     * @return
     */
    private String getId(DeviceNumber deviceNumber) {
        try {
            return PropertyUtils.getProperty(deviceNumber, "id").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*--------------------------------子表处理-设备编号-end----------------------------------------------*/

     /*--------------------------------子表处理-微信用户-begin----------------------------------------------*/
     /**
      * 查询子表信息 会传入主表ID
      * @return
      */
     @GetMapping(value = "/listWxUserByMainId")
     public Result<?> listWxUserByMainId(WxUser wxUser,
                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                         @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                         HttpServletRequest req) {
         if (StringUtils.isNotBlank(wxUser.getNickname())) {
             wxUser.setNickname(wxUser.getNickname().trim());
         }
         QueryWrapper<WxUser> queryWrapper = QueryGenerator.initQueryWrapper(wxUser, req.getParameterMap());
         if (StringUtils.isNotBlank(wxUser.getNickname())) {
             queryWrapper.like("nickname",wxUser.getNickname());
         }
         Page<WxUser> page = new Page<WxUser>(pageNo, pageSize);
         IPage<WxUser> pageList = wxUserService.page(page, queryWrapper);
         return Result.ok(pageList);
     }

     /**
      * 添加
      * @param wxUser
      * @return
      */
     @PostMapping(value = "/addWxUser")
     public Result<?> addWxUser(@RequestBody WxUser wxUser) {
         wxUserService.save(wxUser);
         return Result.ok("添加成功！");
     }

     /**
      * 编辑
      * @param wxUser
      * @return
      */
     @PutMapping(value = "/editWxUser")
     public Result<?> editWxUser(@RequestBody WxUser wxUser) {
         wxUserService.updateById(wxUser);
         // 若注册表有用户信息则改为绑定状态
         if (StringUtils.isNotBlank(wxUser.getClientId())) {
             QueryWrapper<WxUserRegister> wxUserRegisterQueryWrapper = new QueryWrapper<>();
             wxUserRegisterQueryWrapper.eq("open_id",wxUser.getOpenId());
             WxUserRegister wxUserRegister = wxUserRegisterService.getOne(wxUserRegisterQueryWrapper);
             if (oConvertUtils.isNotEmpty(wxUserRegister)) {
                 wxUserRegister.setStatus("1");
                 wxUserRegisterService.updateById(wxUserRegister);
             }
         }
         return Result.ok("编辑成功!");
     }

     /**
      * 通过id删除
      * @param id
      * @return
      */
     @DeleteMapping(value = "/deleteWxUser")
     public Result<?> deleteWxUser(@RequestParam(name="id",required=true) String id) {
         wxUserService.removeById(id);
         return Result.ok("删除成功!");
     }

     /**
      * 批量删除
      * @param ids
      * @return
      */
     @DeleteMapping(value = "/deleteBatchWxUser")
     public Result<?> deleteBatchWxUser(@RequestParam(name="ids",required=true) String ids) {
         this.wxUserService.removeByIds(Arrays.asList(ids.split(",")));
         return Result.ok("批量删除成功!");
     }

     /**
      * 同步用户
      * @return
      */
     @GetMapping(value = "/synchronizeWxUser")
     public Result<?> synchronizeWxUser() {
         try {
             List<WxUserEntity> wxuserList = WxUserUtils.getAllWxuser(redisUtil.get(WechatConstant.ACCESS_TOKEN_KEY).toString(),null);
             for (WxUserEntity wxUserEntity : wxuserList) {
                 QueryWrapper<WxUser> wxUserQueryWrapper = new QueryWrapper<>();
                 wxUserQueryWrapper.eq("open_id", wxUserEntity.getOpenid());
                 WxUser editWxUser = wxUserService.getOne(wxUserQueryWrapper);
                 if (oConvertUtils.isEmpty(editWxUser)) {
                     WxUser addWxUser = new WxUser();
                     addWxUser.setOpenId(wxUserEntity.getOpenid());
                     addWxUser.setNickname(wxUserEntity.getNickname());
                     addWxUser.setSex(wxUserEntity.getSex());
                     addWxUser.setCity(wxUserEntity.getCity());
                     addWxUser.setCountry(wxUserEntity.getCountry());
                     addWxUser.setProvince(wxUserEntity.getProvince());
                     addWxUser.setRemark(wxUserEntity.getRemark());
                     wxUserService.save(addWxUser);
                 } else {
                     editWxUser.setNickname(wxUserEntity.getNickname());
                     editWxUser.setSex(wxUserEntity.getSex());
                     editWxUser.setCity(wxUserEntity.getCity());
                     editWxUser.setCountry(wxUserEntity.getCountry());
                     editWxUser.setProvince(wxUserEntity.getProvince());
                     editWxUser.setRemark(wxUserEntity.getRemark());
                     wxUserService.updateById(editWxUser);
                 }
             }
         } catch (WexinReqException e) {
             e.printStackTrace();
             log.error("获取微信用户出错!");
             return Result.error("同步失败!");
         }
         return Result.ok("同步成功!");
     }

     /*--------------------------------子表处理-微信用户-end----------------------------------------------*/

}
