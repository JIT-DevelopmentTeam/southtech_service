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
        <a-row class="form-row" :gutter="24">
          <a-col :lg="12">
            <a-form-item label="工程师" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <j-select-user-by-dep v-decorator="['serviceEngineerName',validatorRules.serviceEngineerName]" :disabled="true" :multi="false" :trigger-change="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="12">
            <a-form-item label="预约服务时间" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <j-date placeholder="请选择预约服务时间" v-decorator="[ 'appointment', validatorRules.appointment]" :trigger-change="true" date-format="YYYY-MM-DD" style="width: 100%"/>
            </a-form-item>
          </a-col>
          <a-col :lg="12">
            <a-form-item label="计划完成时间" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <j-date placeholder="请选择计划完成时间" v-decorator="[ 'plannedCompletionTime', validatorRules.plannedCompletionTime]" :trigger-change="true" :show-time="true" date-format="YYYY-MM-DD" style="width: 100%"/>
            </a-form-item>
          </a-col>
          <a-col :lg="12">
            <a-form-item label="同行人员" :labelCol="labelCol" :wrapperCol="wrapperCol">
              <j-select-user-by-dep v-decorator="['peers',validatorRules.peers]" :multi="true" :trigger-change="true"/>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
    import { httpAction,getAction } from '@/api/manage'
    import pick from 'lodash.pick'
    import JDate from '@/components/jeecg/JDate' 
    import JSelectUserByDep from '@/components/jeecgbiz/JitSelectUserByDep'

    export default {
    name: "JitDispatchModal",
    components: {
      JDate,
      JSelectUserByDep
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
        width:1200,
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
        
        serviceEngineerName:{rules: [{ required: true, message: '请选择工程师!' }]},
        appointment:{rules: [{ required: true, message: '请选择预约服务时间!' }]},
        plannedCompletionTime:{rules: [{ required: true, message: '请选择计划完成时间!' }]},
        peers:{}
        },
        url: {
          edit: "/workorder/workOrder/dispatchWorkOrderDetailByIds",
        },
        workOrderDetailId:null
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
        this.workOrderDetailId = this.model.ids;
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model, 'serviceEngineerName'));
        })
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleOk () {
        const that = this;
        // 触发表单验证
        this.form.validateFields(async (err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let httpurl = '';
            let method = '';
            httpurl+=this.url.edit+"?serviceEngineerName="+values.serviceEngineerName+"&appointment="+values.appointment+"&plannedCompletionTime="+values.plannedCompletionTime+"&workOrderDetailId="+this.workOrderDetailId;
            if (values.peers) {
              httpurl += '&peers='+values.peers;
            }
            method = 'post';       
            await httpAction(httpurl,null,method).then((res)=>{
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
        this.form.setFieldsValue(pick(row,'serviceEngineerName','appointment','plannedCompletionTime','peers'))
      },


    }
  }
</script>

<style lang="scss" scoped>

</style>