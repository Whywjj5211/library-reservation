<script setup>
import {ref,computed,onMounted} from 'vue'
import axios from 'axios'
import {ElMessage,ElMessageBox} from 'element-plus'

const logged=ref(false), loginLoading=ref(false)
const username=ref('student'), password=ref('123456')
const user=ref(null), token=ref('')
const active=ref('home'), adminTab=ref('dashboard')

const rooms=ref([]), roomId=ref(null), seats=ref([]), occupied=ref([])
const date=ref(new Date().toISOString().slice(0,10)), start=ref('09:00'), end=ref('11:00')
const selected=ref(null), reservations=ref([]), notices=ref([])
const loading=ref(false)

const stats=ref({users:0,libraries:0,rooms:0,seats:0,todayReservations:0,todayCheckIn:0,activeReservations:0,trend:[]})
const adminUsers=ref([]), adminLibraries=ref([]), adminRooms=ref([]), adminSeats=ref([])
const adminReservations=ref([]), violations=ref([]), adminNotices=ref([])
const dialog=ref({show:false,type:'',title:''}), form=ref({})
const isAdmin=computed(()=>user.value?.role==='ADMIN')

axios.interceptors.request.use(config=>{
  if(token.value) config.headers.Authorization='Bearer '+token.value
  return config
})

const statusText={RESERVED:'待签到',CHECKED_IN:'使用中',COMPLETED:'已完成',CANCELLED:'已取消',NO_SHOW:'未签到'}
const statusType={RESERVED:'warning',CHECKED_IN:'primary',COMPLETED:'success',CANCELLED:'info',NO_SHOW:'danger'}

async function login(){
  loginLoading.value=true
  try{
    const {data}=await axios.post('/api/auth/login',{username:username.value,password:password.value})
    if(data.code!==200) return ElMessage.error(data.message)
    user.value=data.data.user; token.value=data.data.token; logged.value=true
    await loadCommon()
    if(isAdmin.value) {active.value='admin'; await loadAdmin()}
  }catch(e){ElMessage.error('无法连接后端，请先启动 Spring Boot')}
  finally{loginLoading.value=false}
}
async function loadCommon(){
  const [r,n]=await Promise.all([axios.get('/api/rooms'),axios.get('/api/notices')])
  rooms.value=r.data.data||[]; notices.value=n.data.data||[]
  if(!roomId.value && rooms.value.length) roomId.value=rooms.value[0].id
  await loadSeats()
  if(user.value?.id) await loadReservations()
}
async function loadSeats(){
  if(!roomId.value)return
  const [s,o]=await Promise.all([
    axios.get('/api/seats',{params:{roomId:roomId.value}}),
    axios.get('/api/seats/occupied',{params:{roomId:roomId.value,date:date.value,startTime:start.value,endTime:end.value}})
  ])
  seats.value=s.data.data||[]; occupied.value=o.data.data||[]; selected.value=null
}
async function loadReservations(){
  const {data}=await axios.get('/api/reservations',{params:{userId:user.value.id}})
  reservations.value=data.data||[]
}
function pick(s){if(s.status!==1||occupied.value.includes(s.id))return;selected.value=s.id}
async function reserve(){
  if(!selected.value)return ElMessage.warning('请选择绿色空闲座位')
  loading.value=true
  try{
    const {data}=await axios.post('/api/reservations',{userId:user.value.id,seatId:selected.value,reservationDate:date.value,startTime:start.value,endTime:end.value})
    if(data.code!==200)return ElMessage.error(data.message)
    ElMessage.success('预约成功'); await loadReservations(); await loadSeats(); active.value='reservations'
  }finally{loading.value=false}
}
async function cancel(id){
  await ElMessageBox.confirm('确定取消这条预约吗？','提示',{type:'warning'})
  const {data}=await axios.delete('/api/reservations/'+id)
  if(data.code!==200)return ElMessage.error(data.message)
  ElMessage.success('已取消');await loadReservations();await loadSeats()
}
async function action(id,type){
  const {data}=await axios.post(`/api/reservations/${id}/${type}`)
  if(data.code!==200)return ElMessage.error(data.message)
  ElMessage.success(type==='check-in'?'签到成功':'签退成功');await loadReservations();await loadSeats()
}
async function logout(){
  try{await axios.post('/api/auth/logout')}catch(e){}
  logged.value=false;user.value=null;token.value='';active.value='home'
}

async function loadAdmin(){
  const [s,u,l,r,se,res,v,n]=await Promise.all([
    axios.get('/api/admin/stats'),axios.get('/api/admin/users'),axios.get('/api/admin/libraries'),
    axios.get('/api/admin/rooms'),axios.get('/api/admin/seats'),axios.get('/api/admin/reservations'),
    axios.get('/api/admin/violations'),axios.get('/api/admin/notices')
  ])
  stats.value=s.data.data||stats.value;adminUsers.value=u.data.data||[];adminLibraries.value=l.data.data||[]
  adminRooms.value=r.data.data||[];adminSeats.value=se.data.data||[];adminReservations.value=res.data.data||[]
  violations.value=v.data.data||[];adminNotices.value=n.data.data||[]
}
async function adminRefresh(){await loadAdmin();ElMessage.success('数据已刷新')}
function openAdd(type){
  const defaults={library:{name:'',address:'',description:''},room:{libraryId:adminLibraries.value[0]?.id,name:'',floor:'1F',capacity:20},seat:{roomId:adminRooms.value[0]?.id,seatNo:'',rowNo:1,columnNo:1},notice:{title:'',content:''}}
  form.value=defaults[type]||{};dialog.value={show:true,type,title:'新增'+({library:'图书馆',room:'阅览室',seat:'座位',notice:'公告'}[type]||'')}
}
function openEdit(type,row){form.value={...row};dialog.value={show:true,type,title:'编辑'+({library:'图书馆',room:'阅览室',seat:'座位',notice:'公告'}[type]||'')}}
async function saveDialog(){
  const t=dialog.value.type, f=form.value
  const base={library:'/api/admin/libraries',room:'/api/admin/rooms',seat:'/api/admin/seats',notice:'/api/admin/notices'}[t]
  const res=f.id?await axios.put(base+'/'+f.id,f):await axios.post(base,f)
  if(res.data.code!==200)return ElMessage.error(res.data.message)
  dialog.value.show=false;await loadAdmin();ElMessage.success('保存成功')
}
async function del(type,id){
  await ElMessageBox.confirm('删除后可能影响关联数据，确定继续吗？','确认删除',{type:'warning'})
  const base={library:'/api/admin/libraries',room:'/api/admin/rooms',seat:'/api/admin/seats',notice:'/api/admin/notices'}[type]
  const {data}=await axios.delete(base+'/'+id);if(data.code!==200)return ElMessage.error(data.message)
  await loadAdmin();ElMessage.success('删除成功')
}
async function toggleUser(row){
  const status=row.status===1?0:1
  const {data}=await axios.put(`/api/admin/users/${row.id}/status`,null,{params:{status}})
  if(data.code!==200)return ElMessage.error(data.message);await loadAdmin()
}
async function resolveViolation(id){
  const {data}=await axios.put('/api/admin/violations/'+id+'/resolve')
  if(data.code!==200)return ElMessage.error(data.message);await loadAdmin();ElMessage.success('已处理')
}
function seatClass(s){return s.status!==1?'disabled':occupied.value.includes(s.id)?'busy':selected.value===s.id?'selected':''}
function formatStatus(s){return statusText[s]||s}
function shortDate(v){return v?String(v).slice(5,10):''}
onMounted(()=>{})
</script>

<template>
<div v-if="!logged" class="login-page">
  <section class="login-left">
    <div class="login-hero">
      <div class="brand-icon">📚</div>
      <h1>高校图书馆<br>座位预约系统</h1>
      <p>让每一次学习，都有一个舒适、安静、专属的座位。</p>
      <div class="feature-row">
        <span class="feature-pill">实时座位状态</span><span class="feature-pill">在线预约</span>
        <span class="feature-pill">签到签退</span><span class="feature-pill">数据统计</span>
      </div>
    </div>
  </section>
  <section class="login-panel">
    <el-card class="login-card">
      <h2>欢迎回来</h2><p class="login-sub">登录后开始预约你的学习座位</p>
      <el-input v-model="username" size="large" placeholder="账号" class="mb"/>
      <el-input v-model="password" size="large" type="password" show-password placeholder="密码" class="mb"/>
      <el-button type="primary" size="large" class="full" :loading="loginLoading" @click="login">登录系统</el-button>
      <div class="demo-box">学生：student / 123456<br>管理员：admin / 123456</div>
    </el-card>
  </section>
</div>

<div v-else class="shell">
  <header class="topbar">
    <div class="brand"><div class="brand-mark">📖</div><span>高校图书馆座位预约系统</span></div>
    <div class="top-user">
      <el-tag :type="isAdmin?'danger':'primary'">{{isAdmin?'管理员':'学生'}}</el-tag>
      <span>{{user.name}}</span>
      <el-button text @click="logout">退出</el-button>
    </div>
  </header>

  <!-- 学生端 -->
  <div v-if="!isAdmin" class="content">
    <div class="welcome">
      <div><h1>{{active==='home'?'你好，'+user.name:'我的'+(active==='reservations'?'预约':'公告')}}</h1><div class="muted">今天也要认真学习 📚</div></div>
      <el-button v-if="active!=='home'" @click="active='home'">返回首页</el-button>
    </div>

    <div v-if="active==='home'">
      <div class="grid-4">
        <el-card class="stat-card"><span class="stat-label">阅览室</span><el-icon class="stat-icon"><OfficeBuilding/></el-icon><div class="stat-value">{{rooms.length}}</div></el-card>
        <el-card class="stat-card"><span class="stat-label">当前预约</span><el-icon class="stat-icon"><Calendar/></el-icon><div class="stat-value">{{reservations.filter(x=>x.status==='RESERVED'||x.status==='CHECKED_IN').length}}</div></el-card>
        <el-card class="stat-card"><span class="stat-label">我的记录</span><el-icon class="stat-icon"><Tickets/></el-icon><div class="stat-value">{{reservations.length}}</div></el-card>
        <el-card class="stat-card"><span class="stat-label">今日开放</span><el-icon class="stat-icon"><Clock/></el-icon><div class="stat-value">08:00-21:00</div></el-card>
      </div>

      <div class="section-grid">
        <el-card class="panel">
          <template #header><b>快速预约座位</b></template>
          <div class="filter-row">
            <el-select v-model="roomId" style="width:220px" @change="loadSeats"><el-option v-for="r in rooms" :key="r.id" :value="r.id" :label="r.name"/></el-select>
            <el-date-picker v-model="date" value-format="YYYY-MM-DD" type="date" :disabled-date="d=>d<new Date(new Date().setHours(0,0,0,0))" @change="loadSeats"/>
            <el-time-select v-model="start" start="08:00" step="00:30" end="20:00" @change="loadSeats"/>
            <el-time-select v-model="end" start="08:30" step="00:30" end="21:00" @change="loadSeats"/>
          </div>
          <div class="seat-grid">
            <button v-for="s in seats" :key="s.id" :class="['seat-btn',seatClass(s)]" @click="pick(s)">{{s.seatNo}}</button>
          </div>
          <div class="legend"><span><i class="dot green"></i>空闲</span><span><i class="dot red"></i>已预约</span><span><i class="dot blue"></i>已选择</span><span><i class="dot gray"></i>故障</span></div>
          <div style="margin-top:18px;display:flex;justify-content:space-between;align-items:center">
            <span class="muted">{{selected?'已选择 '+seats.find(x=>x.id===selected)?.seatNo:'请选择一个绿色座位'}}</span>
            <el-button type="primary" :loading="loading" @click="reserve">确认预约</el-button>
          </div>
        </el-card>

        <el-card class="panel">
          <template #header><b>图书馆公告</b></template>
          <div v-for="n in notices.slice(0,5)" :key="n.id" class="notice-item">
            <div class="notice-title">{{n.title}}</div><div class="muted" style="margin-top:5px">{{n.content}}</div>
            <div class="notice-time">{{n.createdTime}}</div>
          </div>
          <div v-if="!notices.length" class="empty">暂无公告</div>
        </el-card>
      </div>

      <el-card class="panel" style="margin-top:18px">
        <template #header><div style="display:flex;justify-content:space-between"><b>最近预约</b><el-button text type="primary" @click="active='reservations'">查看全部</el-button></div></template>
        <el-table :data="reservations.slice(0,5)" stripe>
          <el-table-column prop="reservationDate" label="日期"/>
          <el-table-column prop="startTime" label="开始"/>
          <el-table-column prop="endTime" label="结束"/>
          <el-table-column prop="seatId" label="座位"/>
          <el-table-column label="状态"><template #default="{row}"><el-tag :type="statusType[row.status]">{{formatStatus(row.status)}}</el-tag></template></el-table-column>
          <el-table-column label="操作"><template #default="{row}">
            <el-button v-if="row.status==='RESERVED'" link type="primary" @click="action(row.id,'check-in')">签到</el-button>
            <el-button v-if="row.status==='CHECKED_IN'" link type="success" @click="action(row.id,'check-out')">签退</el-button>
            <el-button v-if="row.status==='RESERVED'" link type="danger" @click="cancel(row.id)">取消</el-button>
          </template></el-table-column>
        </el-table>
      </el-card>
    </div>

    <div v-if="active==='reservations'">
      <el-card class="panel"><el-table :data="reservations" stripe>
        <el-table-column prop="id" label="编号" width="75"/><el-table-column prop="reservationDate" label="日期"/>
        <el-table-column prop="startTime" label="开始"/><el-table-column prop="endTime" label="结束"/><el-table-column prop="seatId" label="座位"/>
        <el-table-column label="状态"><template #default="{row}"><el-tag :type="statusType[row.status]">{{formatStatus(row.status)}}</el-tag></template></el-table-column>
        <el-table-column label="操作"><template #default="{row}">
          <el-button v-if="row.status==='RESERVED'" size="small" type="primary" @click="action(row.id,'check-in')">签到</el-button>
          <el-button v-if="row.status==='CHECKED_IN'" size="small" type="success" @click="action(row.id,'check-out')">签退</el-button>
          <el-button v-if="row.status==='RESERVED'" size="small" type="danger" @click="cancel(row.id)">取消</el-button>
        </template></el-table-column>
      </el-table></el-card>
    </div>
  </div>

  <!-- 管理端 -->
  <div v-else class="admin-layout">
    <aside class="sidebar">
      <div class="side-title">管理控制台</div>
      <div v-for="item in [
        ['dashboard','数据概览','DataAnalysis'],['users','用户管理','User'],['libraries','图书馆','OfficeBuilding'],
        ['rooms','阅览室','Reading'],['seats','座位管理','Grid'],['reservations','预约管理','Calendar'],
        ['violations','违规管理','Warning'],['notices','公告管理','Bell']
      ]" :key="item[0]" :class="['side-item',adminTab===item[0]?'active':'']" @click="adminTab=item[0]">
        <el-icon><component :is="item[2]"/></el-icon>{{item[1]}}
      </div>
    </aside>

    <main class="admin-main">
      <div class="welcome"><div><h1>管理控制台</h1><div class="muted">欢迎回来，{{user.name}} · 系统运营数据</div></div><el-button @click="adminRefresh"><el-icon><Refresh/></el-icon>刷新</el-button></div>

      <div v-if="adminTab==='dashboard'">
        <div class="grid-4">
          <el-card class="stat-card"><span class="stat-label">用户总数</span><div class="stat-value">{{stats.users}}</div></el-card>
          <el-card class="stat-card"><span class="stat-label">座位总数</span><div class="stat-value">{{stats.seats}}</div></el-card>
          <el-card class="stat-card"><span class="stat-label">今日预约</span><div class="stat-value">{{stats.todayReservations}}</div></el-card>
          <el-card class="stat-card"><span class="stat-label">今日签到</span><div class="stat-value">{{stats.todayCheckIn}}</div></el-card>
        </div>
        <div class="section-grid">
          <el-card class="panel"><template #header><b>近 7 天预约趋势</b></template>
            <div class="trend">
              <div v-for="x in stats.trend" :key="x.date" class="bar-wrap"><span class="bar-num">{{x.count}}</span><div class="bar" :style="{height:Math.max(5,(x.count/(Math.max(...stats.trend.map(a=>a.count),1)))*130)+'px'}"></div><span class="bar-label">{{x.date}}</span></div>
            </div>
          </el-card>
          <el-card class="panel"><template #header><b>系统概况</b></template>
            <el-descriptions :column="1" border><el-descriptions-item label="图书馆">{{stats.libraries}} 个</el-descriptions-item><el-descriptions-item label="阅览室">{{stats.rooms}} 个</el-descriptions-item><el-descriptions-item label="座位">{{stats.seats}} 个</el-descriptions-item><el-descriptions-item label="进行中预约">{{stats.activeReservations}} 个</el-descriptions-item></el-descriptions>
          </el-card>
        </div>
      </div>

      <div v-if="adminTab==='users'"><el-card class="panel"><template #header><b>用户管理</b></template>
        <el-table :data="adminUsers" stripe><el-table-column prop="id" label="ID" width="70"/><el-table-column prop="studentNo" label="学号"/><el-table-column prop="name" label="姓名"/><el-table-column prop="phone" label="电话"/><el-table-column prop="role" label="角色"/><el-table-column label="状态"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'">{{row.status===1?'正常':'禁用'}}</el-tag></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button size="small" @click="toggleUser(row)">{{row.status===1?'禁用':'启用'}}</el-button></template></el-table-column></el-table>
      </el-card></div>

      <div v-if="adminTab==='libraries'"><el-card class="panel"><template #header><div class="toolbar"><b>图书馆管理</b><el-button type="primary" @click="openAdd('library')">新增图书馆</el-button></div></template>
        <el-table :data="adminLibraries" stripe><el-table-column prop="id" label="ID" width="70"/><el-table-column prop="name" label="名称"/><el-table-column prop="address" label="地址"/><el-table-column prop="description" label="简介"/><el-table-column label="操作"><template #default="{row}"><el-button link @click="openEdit('library',row)">编辑</el-button><el-button link type="danger" @click="del('library',row.id)">删除</el-button></template></el-table-column></el-table>
      </el-card></div>

      <div v-if="adminTab==='rooms'"><el-card class="panel"><template #header><div class="toolbar"><b>阅览室管理</b><el-button type="primary" @click="openAdd('room')">新增阅览室</el-button></div></template>
        <el-table :data="adminRooms" stripe><el-table-column prop="id" label="ID"/><el-table-column prop="libraryId" label="图书馆ID"/><el-table-column prop="name" label="名称"/><el-table-column prop="floor" label="楼层"/><el-table-column prop="capacity" label="容量"/><el-table-column label="操作"><template #default="{row}"><el-button link @click="openEdit('room',row)">编辑</el-button><el-button link type="danger" @click="del('room',row.id)">删除</el-button></template></el-table-column></el-table>
      </el-card></div>

      <div v-if="adminTab==='seats'"><el-card class="panel"><template #header><div class="toolbar"><b>座位管理</b><el-button type="primary" @click="openAdd('seat')">新增座位</el-button></div></template>
        <el-table :data="adminSeats" stripe><el-table-column prop="id" label="ID"/><el-table-column prop="roomId" label="阅览室ID"/><el-table-column prop="seatNo" label="座位号"/><el-table-column prop="rowNo" label="行"/><el-table-column prop="columnNo" label="列"/><el-table-column label="状态"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'">{{row.status===1?'启用':'故障'}}</el-tag></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button link @click="openEdit('seat',row)">编辑</el-button><el-button link type="danger" @click="del('seat',row.id)">删除</el-button></template></el-table-column></el-table>
      </el-card></div>

      <div v-if="adminTab==='reservations'"><el-card class="panel"><template #header><b>预约管理</b></template>
        <el-table :data="adminReservations" stripe><el-table-column prop="id" label="编号"/><el-table-column prop="userId" label="用户ID"/><el-table-column prop="seatId" label="座位ID"/><el-table-column prop="reservationDate" label="日期"/><el-table-column prop="startTime" label="开始"/><el-table-column prop="endTime" label="结束"/><el-table-column label="状态"><template #default="{row}"><el-tag :type="statusType[row.status]">{{formatStatus(row.status)}}</el-tag></template></el-table-column></el-table>
      </el-card></div>

      <div v-if="adminTab==='violations'"><el-card class="panel"><template #header><b>违规管理</b></template>
        <el-table :data="violations" stripe><el-table-column prop="id" label="ID"/><el-table-column prop="userId" label="用户ID"/><el-table-column prop="reservationId" label="预约ID"/><el-table-column prop="reason" label="原因"/><el-table-column prop="createdTime" label="时间"/><el-table-column label="状态"><template #default="{row}"><el-tag :type="row.status===1?'danger':'success'">{{row.status===1?'待处理':'已处理'}}</el-tag></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button v-if="row.status===1" size="small" type="primary" @click="resolveViolation(row.id)">标记已处理</el-button></template></el-table-column></el-table>
      </el-card></div>

      <div v-if="adminTab==='notices'"><el-card class="panel"><template #header><div class="toolbar"><b>公告管理</b><el-button type="primary" @click="openAdd('notice')">发布公告</el-button></div></template>
        <el-table :data="adminNotices" stripe><el-table-column prop="id" label="ID"/><el-table-column prop="title" label="标题"/><el-table-column prop="content" label="内容"/><el-table-column prop="createdTime" label="发布时间"/><el-table-column label="操作"><template #default="{row}"><el-button link @click="openEdit('notice',row)">编辑</el-button><el-button link type="danger" @click="del('notice',row.id)">删除</el-button></template></el-table-column></el-table>
      </el-card></div>
    </main>
  </div>

  <el-dialog v-model="dialog.show" :title="dialog.title" width="520px">
    <div class="dialog-form" v-if="dialog.type==='library'">
      <el-input v-model="form.name" placeholder="图书馆名称"/><el-input v-model="form.address" placeholder="地址"/><el-input v-model="form.description" type="textarea" placeholder="简介"/>
    </div>
    <div class="dialog-form" v-if="dialog.type==='room'">
      <el-select v-model="form.libraryId" placeholder="所属图书馆"><el-option v-for="x in adminLibraries" :key="x.id" :value="x.id" :label="x.name"/></el-select>
      <el-input v-model="form.name" placeholder="阅览室名称"/><el-input v-model="form.floor" placeholder="楼层"/><el-input-number v-model="form.capacity" :min="1" :max="500"/>
    </div>
    <div class="dialog-form" v-if="dialog.type==='seat'">
      <el-select v-model="form.roomId" placeholder="所属阅览室"><el-option v-for="x in adminRooms" :key="x.id" :value="x.id" :label="x.name"/></el-select>
      <el-input v-model="form.seatNo" placeholder="座位号"/><el-input-number v-model="form.rowNo" :min="1"/><el-input-number v-model="form.columnNo" :min="1"/>
      <el-select v-model="form.status"><el-option :value="1" label="启用"/><el-option :value="0" label="故障"/></el-select>
    </div>
    <div class="dialog-form" v-if="dialog.type==='notice'">
      <el-input v-model="form.title" placeholder="公告标题"/><el-input v-model="form.content" type="textarea" :rows="6" placeholder="公告内容"/>
    </div>
    <template #footer><el-button @click="dialog.show=false">取消</el-button><el-button type="primary" @click="saveDialog">保存</el-button></template>
  </el-dialog>
</div>
</template>
