<template>
  <a-modal
    :title="title"
    :width="width"
    :visible="visible"
    :confirmLoading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
    cancelText="关闭">
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">

        <a-form-item label="编号" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-decorator="[ 'number', validatorRules.number]" placeholder="请输入编号"></a-input>
        </a-form-item>
        <a-form-item label="名称" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-decorator="[ 'name', validatorRules.name]" placeholder="请输入名称"></a-input>
        </a-form-item>
        <a-form-item label="类型" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-input v-decorator="[ 'type', validatorRules.type]" placeholder="请输入类型"></a-input>
        </a-form-item>
        <a-form-item label="描述" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <a-textarea rows="4" placeholder="请输入描述" v-decorator="[ 'description', validatorRules.description]" style="resize:none;"/>
        </a-form-item>
        <a-form-item label="保质期至" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <j-date placeholder="请选择保质期至" v-decorator="[ 'qgp', validatorRules.qgp]" :trigger-change="true" style="width: 100%"/>
        </a-form-item>
        <a-form-item label="签约日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <j-date placeholder="请选择签约日期" v-decorator="[ 'signing', validatorRules.signing]" :trigger-change="true" style="width: 100%"/>
        </a-form-item>
        <a-form-item label="验收日期" :labelCol="labelCol" :wrapperCol="wrapperCol">
          <j-date placeholder="请选择验收日期" v-decorator="[ 'acceptance', validatorRules.acceptance]" :trigger-change="true" style="width: 100%"/>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>

  import { httpAction } from '@/api/manage'
  import pick from 'lodash.pick'
  import { validateDuplicateValue } from '@/utils/util'
  import JDate from '@/components/jeecg/JDate'  
  import JSearchSelectTag from '@/components/dict/JSearchSelectTag'

  export default {
    name: "DeviceNumberModal",
    components: {
      JDate,
      JSearchSelectTag
    },
    props:{
      mainId:{
        type:String,
        required:false,
        default:''
      }
    },
    data () {
      return {
        form: this.$form.createForm(this),
        title:"操作",
        width:800,
        visible: false,
        model: {},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },

        confirmLoading: false,
        validatorRules:{
        number:{rules: [{validator: (rule, value, callback) => validateDuplicateValue('tb_device_number', 'number', value, this.model.id, callback)}]},
        name:{rules: [{ required: true, message: '请输入名称!' }]},
        type:{rules: [{ required: true, message: '请输入类型!' }]},
        description:{},
        qgp:{},
        signing:{},
        acceptance:{}
        },
        url: {
          add: "/client/client/addDeviceNumber",
          edit: "/client/client/editDeviceNumber",
        }

      }
    },
    created () {
    },
    methods: {
      add () {
        this.edit({});
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'createBy','createTime','updateBy','updateTime','sysOrgCode','number','name','type','description','qgp','signing','acceptance','clientId'))
        })
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleOk () {
        const that = this;
        // 触发表单验证
        this.form.validateFields((err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let httpurl = '';
            let method = '';
            if(!this.model.id){
              httpurl+=this.url.add;
              method = 'post';
            }else{
              httpurl+=this.url.edit;
               method = 'put';
            }
            let formData = Object.assign(this.model, values);
            formData['clientId'] = this.mainId
            console.log("表单提交数据",formData)
            httpAction(httpurl,formData,method).then((res)=>{
              if(res.success){
                that.$message.success(res.message);
                that.$emit('ok');
              }else{
                that.$message.warning(res.message);
              }
            }).finally(() => {
              that.confirmLoading = false;
              that.close();
            })
          }

        })
      },
      handleCancel () {
        this.close()
      },
      popupCallback(row){
        this.form.setFieldsValue(pick(row,'createBy','createTime','updateBy','updateTime','sysOrgCode','number','name','type','description','qgp','signing','acceptance','clientId'))
      },


    }
  }
</script>
