<template>
  <a-card :bordered="false">
    <a-row :gutter="15">
      <!-- 左侧树 -->
      <a-col :span="5">
        <a-tree
          showLine
          :treeData="treeData"
          :selectedKeys="selectedKeys"
          :style="{'height':'590px','border-right':'2px solid #c1c1c1','overflow-y':'auto'}"
          @select="this.onSelect"
        >
        </a-tree>
      </a-col>

      <!--右侧列表-->
      <a-col :span="19">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="8">
                <a-form-item label="名称">
                  <a-input placeholder="请输入名称" v-model="queryParam.name"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="8">
                <a-form-item label="规格">
                  <a-input placeholder="请输入规格" v-model="queryParam.model"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="8">
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery" icon="search">查询</a-button>
                  <a-button type="primary" @click="searchReset" icon="reload" style="margin-left: 8px">重置</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? '收起' : '展开' }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </span>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 查询区域-END -->

        <!-- 操作按钮区域 -->
        <div class="table-operator">
          <a-button @click="sync" type="primary" icon="cloud-download">同步</a-button>
          <!--<a-button type="primary" icon="download" @click="handleExportXls('物料')">导出</a-button>
          <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
            <a-button type="primary" icon="import">导入</a-button>
          </a-upload>-->
          <a-dropdown v-if="selectedRowKeys.length > 0">
            <a-menu slot="overlay">
              <a-menu-item key="1" @click="batchDel">
                <a-icon type="delete"/>
                删除
              </a-menu-item>
            </a-menu>
            <a-button style="margin-left: 8px"> 批量操作
              <a-icon type="down"/>
            </a-button>
          </a-dropdown>
        </div>

        <!-- table区域-begin -->
        <div>
          <div class="ant-alert ant-alert-info" style="margin-bottom: 16px;">
            <i class="anticon anticon-info-circle ant-alert-icon"></i> 已选择 <a style="font-weight: 600">{{
            selectedRowKeys.length }}</a>项
            <a style="margin-left: 24px" @click="onClearSelected">清空</a>
          </div>

          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :pagination="ipagination"
            :loading="loading"
            :rowSelection="{fixed:true,selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :scroll="tableScroll"
            @change="handleTableChange">

            <template slot="htmlSlot" slot-scope="text">
              <div v-html="text"></div>
            </template>
            <template slot="imgSlot" slot-scope="text">
              <span v-if="!text" style="font-size: 12px;font-style: italic;">无此图片</span>
              <img v-else :src="getImgView(text)" height="25px" alt="图片不存在"
                   style="max-width:80px;font-size: 12px;font-style: italic;"/>
            </template>
            <template slot="fileSlot" slot-scope="text">
              <span v-if="!text" style="font-size: 12px;font-style: italic;">无此文件</span>
              <a-button
                v-else
                :ghost="true"
                type="primary"
                icon="download"
                size="small"
                @click="uploadFile(text)">
                下载
              </a-button>
            </template>

            <span slot="action" slot-scope="text, record">
          <a @click="handleEdit(record)">编辑</a>

          <a-divider type="vertical"/>
          <a-dropdown>
            <a class="ant-dropdown-link">更多 <a-icon type="down"/></a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-popconfirm title="确定删除吗?" @confirm="() => handleDelete(record.id)">
                  <a>删除</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </span>

          </a-table>
        </div>

        <item-modal ref="modalForm" @ok="modalFormOk"></item-modal>
      </a-col>
    </a-row>
  </a-card>
</template>

<script>

  import {JeecgListMixin} from '@/mixins/JeecgListMixin'
  import ItemModal from './modules/ItemModal'
  import {getAction} from '@/api/manage'

  export default {
    name: "ItemList",
    mixins: [JeecgListMixin],
    components: {
      ItemModal
    },
    data() {
      return {
        description: '物料管理页面',
        //数据集
        treeData: [],
        selectedKeys: [],
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key: 'rowIndex',
            width: 60,
            align: "center",
            customRender: function (t, r, index) {
              return parseInt(index) + 1;
            }
          },
          {
            title: '物料名称',
            align: "center",
            dataIndex: 'name'
          },
          {
            title: '物料编码',
            align: "center",
            dataIndex: 'number'
          },
          {
            title: '规格型号',
            align: "center",
            dataIndex: 'model'
          },
          {
            title: '计量单位',
            align: "center",
            dataIndex: 'uname'
          },
          {
            title: '所在仓位',
            align: "center",
            dataIndex: 'sname'
          },
          {
            title: '保修期',
            align: "center",
            dataIndex: 'ikfperiod'
          },
          {
            title: '操作',
            dataIndex: 'action',
            align: "center",
            fixed: "right",
            width: 147,
            scopedSlots: {customRender: 'action'}
          }
        ],
        url: {
          list: "/item/item/list",
          treeList: "/itemclass/itemClass/childList",
          delete: "/item/item/delete",
          deleteBatch: "/item/item/deleteBatch",
          exportXlsUrl: "/item/item/exportXls",
          importExcelUrl: "item/item/importExcel",
          syncUrl: "/item/item/sync"
        },
        dictOptions: {},
        tableScroll: {x: 7 * 147 + 50}
      }
    },
    created() {
      this.loadTree();
    },
    computed: {
      importExcelUrl: function () {
        return `${window._CONFIG['domianURL']}/${this.url.importExcelUrl}`;
      }
    },
    methods: {
      getChild(obj, parentId, dataList) {
        var childList = dataList.filter(e => e.pid == parentId);
        var children = [];
        for (let i = 0; i < childList.length; i++) {
          var child = {};
          child.title = childList[i].name;
          child.key = childList[i].number;
          var childAllNode = this.getChild(child, childList[i].id, dataList)
          children.push(childAllNode);
        }
        obj.children = children;
        return obj;
      },
      initDictConfig() {
      },
      onSelect(selectedKeys, info) {
        this.queryParam.number = selectedKeys[0];
        this.loadData();
      },
      loadTree() {
        getAction(this.url.treeList).then(res => {
          var dataList = res.result;
          var parentList = dataList.filter(e => e.pid == 0);
          var resultList = [];
          for (let i = 0; i < parentList.length; i++) {
            let obj = {};
            obj.title = parentList[i].name;
            obj.key = parentList[i].number;
            var allNode = this.getChild(obj, parentList[i].id, dataList);
            resultList.push(allNode);
          }
          this.treeData = resultList;
        })
      },
      sync() {
        this.$confirm({
          title: '同步物料信息',
          content: `同步需要时间,您确定要同步吗?`,
          onOk: () => {
            this.loading = true;
            var ws = new WebSocket('ws://localhost:8080');
            
            getAction(this.url.syncUrl)
              .then(res => {
                if (res.success) {
                  this.$message.success(res.message);
                  this.loadTree();
                  this.loadData();
                } else {
                  this.$message.error(res.message);
                }
              })
              .catch(err => {
                console.log("--->", err);
                this.$message.error("请求超时");
              })
              .finally(() => {
                this.loading = false;
              })
          }
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>