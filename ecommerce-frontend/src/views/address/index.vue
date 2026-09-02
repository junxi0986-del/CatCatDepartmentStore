<template>
  <div class="address">
    <h2>地址管理</h2>
    <el-button type="primary" @click="handleAddAddress">添加地址</el-button>
    <el-table :data="addressList" style="width: 100%">
      <el-table-column prop="receiver" label="收货人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column label="地址">
        <template #default="scope">
          {{ scope.row.province }}{{ scope.row.city }}{{ scope.row.area }}{{ scope.row.detailAddress }}
        </template>
      </el-table-column>
      <el-table-column prop="isDefault" label="默认" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefault === 1">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="scope">
          <el-button size="small" @click="handleEditAddress(scope.row)">编辑</el-button>
          <el-button size="small" @click="handleSetDefault(scope.row.id)">设为默认</el-button>
          <el-button size="small" type="danger" @click="handleDeleteAddress(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑地址对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="addressForm" :rules="rules" ref="addressFormRef" label-width="80px">
        <el-form-item label="收货人" prop="receiver">
          <el-input v-model="addressForm.receiver" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-select v-model="addressForm.province" placeholder="请选择省份" @change="handleProvinceChange">
            <el-option v-for="province in provinces" :key="province" :label="province" :value="province" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-select v-model="addressForm.city" placeholder="请选择城市" @change="handleCityChange">
            <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
          </el-select>
        </el-form-item>
        <el-form-item label="区县" prop="area">
          <el-select v-model="addressForm.area" placeholder="请选择区县">
            <el-option v-for="area in areas" :key="area" :label="area" :value="area" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="addressForm.detailAddress" placeholder="请输入详细地址" type="textarea" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="addressForm.isDefault" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="智能识别">
          <el-input v-model="smartInput" placeholder="请输入完整地址信息，例如：张三 13800138000 北京市朝阳区建国路88号" type="textarea" />
          <el-button type="primary" @click="smartRecognize" style="margin-top: 10px">智能识别</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getAddressList, addAddress, updateAddress, deleteAddress } from '../../api/address'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  setup() {
    const addressList = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('添加地址')
    const addressForm = ref({
      id: '',
      receiver: '',
      phone: '',
      province: '',
      city: '',
      area: '',
      detailAddress: '',
      isDefault: 0
    })
    const addressFormRef = ref(null)
    const smartInput = ref('')

    // 省份城市区县数据源
    const provinces = ref([
      '北京市', '上海市', '广东省', '江苏省', '浙江省', '山东省', '四川省', '湖北省', '河南省', '福建省',
      '河北省', '湖南省', '安徽省', '陕西省', '黑龙江省', '辽宁省', '江西省', '山西省', '广西壮族自治区',
      '云南省', '内蒙古自治区', '吉林省', '贵州省', '新疆维吾尔自治区', '甘肃省', '海南省', '宁夏回族自治区',
      '青海省', '西藏自治区', '香港特别行政区', '澳门特别行政区', '台湾省'
    ])

    const cityData = ref({
      '北京市': ['北京市'],
      '上海市': ['上海市'],
      '广东省': ['广州市', '深圳市', '东莞市', '佛山市', '珠海市', '中山市', '惠州市', '汕头市', '江门市', '湛江市', '茂名市', '肇庆市', '清远市', '韶关市', '河源市', '阳江市', '汕尾市', '潮州市', '揭阳市', '云浮市'],
      '江苏省': ['南京市', '苏州市', '无锡市', '常州市', '南通市', '徐州市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市'],
      '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市'],
      '山东省': ['济南市', '青岛市', '淄博市', '枣庄市', '东营市', '烟台市', '潍坊市', '济宁市', '泰安市', '威海市', '日照市', '临沂市', '德州市', '聊城市', '滨州市', '菏泽市'],
      '四川省': ['成都市', '自贡市', '攀枝花市', '泸州市', '德阳市', '绵阳市', '广元市', '遂宁市', '内江市', '乐山市', '南充市', '眉山市', '宜宾市', '广安市', '达州市', '雅安市', '巴中市', '资阳市', '阿坝藏族羌族自治州', '甘孜藏族自治州', '凉山彝族自治州'],
      '湖北省': ['武汉市', '黄石市', '十堰市', '宜昌市', '襄阳市', '鄂州市', '荆门市', '孝感市', '荆州市', '黄冈市', '咸宁市', '随州市', '恩施土家族苗族自治州', '仙桃市', '潜江市', '天门市', '神农架林区'],
      '河南省': ['郑州市', '开封市', '洛阳市', '平顶山市', '安阳市', '鹤壁市', '新乡市', '焦作市', '濮阳市', '许昌市', '漯河市', '三门峡市', '南阳市', '商丘市', '信阳市', '周口市', '驻马店市', '济源市'],
      '福建省': ['福州市', '厦门市', '莆田市', '三明市', '泉州市', '漳州市', '南平市', '龙岩市', '宁德市'],
      '河北省': ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市'],
      '湖南省': ['长沙市', '株洲市', '湘潭市', '衡阳市', '邵阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西土家族苗族自治州'],
      '安徽省': ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '六安市', '亳州市', '池州市', '宣城市'],
      '陕西省': ['西安市', '铜川市', '宝鸡市', '咸阳市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市'],
      '黑龙江省': ['哈尔滨市', '齐齐哈尔市', '鸡西市', '鹤岗市', '双鸭山市', '大庆市', '伊春市', '佳木斯市', '七台河市', '牡丹江市', '黑河市', '绥化市', '大兴安岭地区'],
      '辽宁省': ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市'],
      '江西省': ['南昌市', '九江市', '景德镇市', '萍乡市', '新余市', '鹰潭市', '赣州市', '宜春市', '上饶市', '吉安市', '抚州市'],
      '山西省': ['太原市', '大同市', '阳泉市', '长治市', '晋城市', '朔州市', '晋中市', '运城市', '忻州市', '临汾市', '吕梁市'],
      '广西壮族自治区': ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市'],
      '云南省': ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄彝族自治州', '红河哈尼族彝族自治州', '文山壮族苗族自治州', '西双版纳傣族自治州', '大理白族自治州', '德宏傣族景颇族自治州', '怒江傈僳族自治州', '迪庆藏族自治州'],
      '内蒙古自治区': ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟'],
      '吉林省': ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延边朝鲜族自治州'],
      '贵州省': ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南布依族苗族自治州', '黔东南苗族侗族自治州', '黔南布依族苗族自治州'],
      '新疆维吾尔自治区': ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉回族自治州', '博尔塔拉蒙古自治州', '巴音郭楞蒙古自治州', '阿克苏地区', '克孜勒苏柯尔克孜自治州', '喀什地区', '和田地区', '伊犁哈萨克自治州', '塔城地区', '阿勒泰地区', '石河子市', '阿拉尔市', '图木舒克市', '五家渠市', '北屯市', '铁门关市', '双河市', '可克达拉市', '昆玉市', '胡杨河市', '新星市'],
      '甘肃省': ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏回族自治州', '甘南藏族自治州'],
      '海南省': ['海口市', '三亚市', '三沙市', '儋州市', '五指山市', '文昌市', '琼海市', '万宁市', '东方市', '定安县', '屯昌县', '澄迈县', '临高县', '白沙黎族自治县', '昌江黎族自治县', '乐东黎族自治县', '陵水黎族自治县', '保亭黎族苗族自治县', '琼中黎族苗族自治县'],
      '宁夏回族自治区': ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市'],
      '青海省': ['西宁市', '海东市', '海北藏族自治州', '黄南藏族自治州', '海南藏族自治州', '果洛藏族自治州', '玉树藏族自治州', '海西蒙古族藏族自治州'],
      '西藏自治区': ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区'],
      '香港特别行政区': ['香港岛', '九龙', '新界'],
      '澳门特别行政区': ['澳门半岛', '氹仔岛', '路环岛'],
      '台湾省': ['台北市', '新北市', '桃园市', '台中市', '台南市', '高雄市', '基隆市', '新竹市', '嘉义市', '新竹县', '苗栗县', '彰化县', '南投县', '云林县', '嘉义县', '屏东县', '宜兰县', '花莲县', '台东县', '澎湖县']
    })

    const areaData = ref({
      // 直辖市
      '北京市': ['东城区', '西城区', '朝阳区', '丰台区', '石景山区', '海淀区', '门头沟区', '房山区', '通州区', '顺义区', '昌平区', '大兴区', '怀柔区', '平谷区', '密云区', '延庆区'],
      '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区', '虹口区', '杨浦区', '浦东新区', '闵行区', '宝山区', '嘉定区', '金山区', '松江区', '青浦区', '奉贤区', '崇明区'],
      '天津市': ['和平区', '河东区', '河西区', '南开区', '河北区', '红桥区', '东丽区', '西青区', '津南区', '北辰区', '武清区', '宝坻区', '滨海新区', '宁河区', '静海区', '蓟州区'],
      '重庆市': ['万州区', '涪陵区', '渝中区', '大渡口区', '江北区', '沙坪坝区', '九龙坡区', '南岸区', '北碚区', '綦江区', '大足区', '渝北区', '巴南区', '黔江区', '长寿区', '江津区', '合川区', '永川区', '南川区', '璧山区', '铜梁区', '潼南区', '荣昌区', '开州区', '梁平区', '武隆区', '城口县', '丰都县', '垫江县', '忠县', '云阳县', '奉节县', '巫山县', '巫溪县', '石柱土家族自治县', '秀山土家族苗族自治县', '酉阳土家族苗族自治县', '彭水苗族土家族自治县'],
      
      // 广东省
      '广州市': ['越秀区', '海珠区', '荔湾区', '天河区', '白云区', '黄埔区', '番禺区', '花都区', '南沙区', '从化区', '增城区'],
      '深圳市': ['罗湖区', '福田区', '南山区', '宝安区', '龙岗区', '盐田区', '龙华区', '坪山区', '光明区', '大鹏新区'],
      '东莞市': ['莞城区', '东城区', '南城区', '万江区', '石龙镇', '石排镇', '茶山镇', '企石镇', '桥头镇', '东坑镇', '横沥镇', '常平镇', '寮步镇', '大朗镇', '黄江镇', '清溪镇', '塘厦镇', '凤岗镇', '长安镇', '虎门镇', '厚街镇', '沙田镇', '道滘镇', '洪梅镇', '麻涌镇', '中堂镇', '高埗镇', '樟木头镇', '大岭山镇', '望牛墩镇'],
      '佛山市': ['禅城区', '南海区', '顺德区', '三水区', '高明区'],
      '珠海市': ['香洲区', '斗门区', '金湾区'],
      '中山市': ['石岐区', '东区', '西区', '南区', '五桂山区', '火炬开发区', '小榄镇', '古镇镇', '横栏镇', '东升镇', '港口镇', '沙溪镇', '大涌镇', '黄圃镇', '南头镇', '东凤镇', '阜沙镇', '三角镇', '民众镇', '南朗镇', '三乡镇', '坦洲镇', '板芙镇', '神湾镇', '翠亨新区'],
      '惠州市': ['惠城区', '惠阳区', '博罗县', '惠东县', '龙门县'],
      '汕头市': ['金平区', '龙湖区', '澄海区', '濠江区', '潮阳区', '潮南区', '南澳县'],
      '江门市': ['蓬江区', '江海区', '新会区', '台山市', '开平市', '鹤山市', '恩平市'],
      '湛江市': ['赤坎区', '霞山区', '坡头区', '麻章区', '廉江市', '雷州市', '吴川市', '遂溪县', '徐闻县'],
      '茂名市': ['茂南区', '电白区', '高州市', '化州市', '信宜市'],
      '肇庆市': ['端州区', '鼎湖区', '高要区', '四会市', '广宁县', '怀集县', '封开县', '德庆县'],
      '清远市': ['清城区', '清新区', '英德市', '连州市', '佛冈县', '阳山县', '连山壮族瑶族自治县', '连南瑶族自治县'],
      '韶关市': ['武江区', '浈江区', '曲江区', '乐昌市', '南雄市', '始兴县', '仁化县', '翁源县', '乳源瑶族自治县', '新丰县'],
      '河源市': ['源城区', '东源县', '和平县', '龙川县', '紫金县', '连平县'],
      '阳江市': ['江城区', '阳东区', '阳春市', '阳西县'],
      '汕尾市': ['城区', '陆丰市', '海丰县', '陆河县'],
      '潮州市': ['湘桥区', '潮安区', '饶平县'],
      '揭阳市': ['榕城区', '揭东区', '普宁市', '揭西县', '惠来县'],
      '云浮市': ['云城区', '云安区', '罗定市', '新兴县', '郁南县'],
      
      // 江苏省
      '南京市': ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区', '栖霞区', '雨花台区', '江宁区', '六合区', '溧水区', '高淳区'],
      '苏州市': ['姑苏区', '虎丘区', '吴中区', '相城区', '吴江区', '苏州工业园区', '常熟市', '张家港市', '昆山市', '太仓市'],
      '无锡市': ['梁溪区', '锡山区', '惠山区', '滨湖区', '新吴区', '江阴市', '宜兴市'],
      '常州市': ['天宁区', '钟楼区', '新北区', '武进区', '金坛区', '溧阳市'],
      '南通市': ['崇川区', '港闸区', '通州区', '启东市', '如皋市', '海门市', '海安市', '如东县'],
      '徐州市': ['云龙区', '鼓楼区', '贾汪区', '泉山区', '铜山区', '新沂市', '邳州市', '丰县', '沛县', '睢宁县'],
      '连云港市': ['连云区', '海州区', '赣榆区', '东海县', '灌云县', '灌南县'],
      '淮安市': ['清江浦区', '淮安区', '淮阴区', '洪泽区', '涟水县', '盱眙县', '金湖县'],
      '盐城市': ['亭湖区', '盐都区', '大丰区', '东台市', '响水县', '滨海县', '阜宁县', '射阳县', '建湖县'],
      '扬州市': ['广陵区', '邗江区', '江都区', '仪征市', '高邮市', '宝应县'],
      
      // 浙江省
      '杭州市': ['上城区', '下城区', '江干区', '拱墅区', '西湖区', '滨江区', '萧山区', '余杭区', '富阳区', '临安区', '桐庐县', '淳安县', '建德市'],
      '宁波市': ['海曙区', '江北区', '北仑区', '镇海区', '鄞州区', '奉化区', '余姚市', '慈溪市', '象山县', '宁海县'],
      '温州市': ['鹿城区', '龙湾区', '瓯海区', '洞头区', '瑞安市', '乐清市', '永嘉县', '平阳县', '苍南县', '文成县', '泰顺县'],
      '嘉兴市': ['南湖区', '秀洲区', '海宁市', '平湖市', '桐乡市', '嘉善县', '海盐县'],
      '湖州市': ['吴兴区', '南浔区', '德清县', '长兴县', '安吉县'],
      '绍兴市': ['越城区', '柯桥区', '上虞区', '诸暨市', '嵊州市', '新昌县'],
      '金华市': ['婺城区', '金东区', '兰溪市', '义乌市', '东阳市', '永康市', '武义县', '浦江县', '磐安县'],
      '衢州市': ['柯城区', '衢江区', '江山市', '常山县', '开化县', '龙游县'],
      '舟山市': ['定海区', '普陀区', '岱山县', '嵊泗县'],
      '台州市': ['椒江区', '黄岩区', '路桥区', '温岭市', '临海市', '玉环市', '三门县', '天台县', '仙居县'],
      
      // 山东省
      '济南市': ['历下区', '市中区', '槐荫区', '天桥区', '历城区', '长清区', '章丘区', '济阳区', '莱芜区', '钢城区', '平阴县', '商河县'],
      '青岛市': ['市南区', '市北区', '黄岛区', '崂山区', '李沧区', '城阳区', '即墨区', '胶州市', '平度市', '莱西市'],
      '淄博市': ['淄川区', '张店区', '博山区', '临淄区', '周村区', '桓台县', '高青县', '沂源县'],
      '枣庄市': ['市中区', '薛城区', '峄城区', '台儿庄区', '山亭区', '滕州市'],
      '东营市': ['东营区', '河口区', '垦利区', '利津县', '广饶县'],
      '烟台市': ['芝罘区', '福山区', '牟平区', '莱山区', '蓬莱区', '龙口市', '莱阳市', '莱州市', '招远市', '栖霞市', '海阳市'],
      '潍坊市': ['潍城区', '寒亭区', '坊子区', '奎文区', '青州市', '诸城市', '寿光市', '安丘市', '高密市', '昌邑市', '临朐县', '昌乐县'],
      '济宁市': ['任城区', '兖州区', '曲阜市', '邹城市', '微山县', '鱼台县', '金乡县', '嘉祥县', '汶上县', '泗水县', '梁山县'],
      '泰安市': ['泰山区', '岱岳区', '新泰市', '肥城市', '宁阳县', '东平县'],
      '威海市': ['环翠区', '文登区', '荣成市', '乳山市'],
      
      // 四川省
      '成都市': ['锦江区', '青羊区', '金牛区', '武侯区', '成华区', '龙泉驿区', '青白江区', '新都区', '温江区', '双流区', '郫都区', '大邑县', '蒲江县', '新津县', '金堂县', '都江堰市', '彭州市', '邛崃市', '崇州市', '简阳市'],
      '绵阳市': ['涪城区', '游仙区', '安州区', '江油市', '三台县', '盐亭县', '梓潼县', '北川羌族自治县', '平武县'],
      '德阳市': ['旌阳区', '罗江区', '广汉市', '什邡市', '绵竹市', '中江县'],
      '自贡市': ['自流井区', '贡井区', '大安区', '沿滩区', '荣县', '富顺县'],
      '泸州市': ['江阳区', '纳溪区', '龙马潭区', '泸县', '合江县', '叙永县', '古蔺县'],
      '内江市': ['市中区', '东兴区', '隆昌市', '威远县', '资中县'],
      '乐山市': ['市中区', '沙湾区', '五通桥区', '金口河区', '峨眉山市', '犍为县', '井研县', '夹江县', '沐川县', '峨边彝族自治县', '马边彝族自治县'],
      '南充市': ['顺庆区', '高坪区', '嘉陵区', '阆中市', '南部县', '营山县', '蓬安县', '仪陇县', '西充县'],
      '宜宾市': ['翠屏区', '南溪区', '叙州区', '江安县', '长宁县', '高县', '珙县', '筠连县', '兴文县', '屏山县'],
      
      // 湖北省
      '武汉市': ['江岸区', '江汉区', '硚口区', '汉阳区', '武昌区', '青山区', '洪山区', '东西湖区', '汉南区', '蔡甸区', '江夏区', '黄陂区', '新洲区'],
      '襄阳市': ['襄城区', '樊城区', '襄州区', '老河口市', '枣阳市', '宜城市', '南漳县', '谷城县', '保康县'],
      '宜昌市': ['西陵区', '伍家岗区', '点军区', '猇亭区', '夷陵区', '宜都市', '枝江市', '当阳市', '远安县', '兴山县', '秭归县', '长阳土家族自治县', '五峰土家族自治县'],
      '黄石市': ['黄石港区', '西塞山区', '下陆区', '铁山区', '大冶市', '阳新县'],
      '十堰市': ['茅箭区', '张湾区', '郧阳区', '丹江口市', '郧西县', '竹山县', '竹溪县', '房县'],
      '荆州市': ['沙市区', '荆州区', '石首市', '洪湖市', '松滋市', '公安县', '监利县', '江陵县'],
      '荆门市': ['东宝区', '掇刀区', '钟祥市', '京山市', '沙洋县'],
      '孝感市': ['孝南区', '应城市', '安陆市', '汉川市', '孝昌县', '大悟县', '云梦县'],
      '黄冈市': ['黄州区', '麻城市', '武穴市', '团风县', '红安县', '罗田县', '英山县', '浠水县', '蕲春县', '黄梅县'],
      
      // 河南省
      '郑州市': ['中原区', '二七区', '管城回族区', '金水区', '上街区', '惠济区', '中牟县', '巩义市', '荥阳市', '新密市', '新郑市', '登封市'],
      '洛阳市': ['老城区', '西工区', '瀍河回族区', '涧西区', '吉利区', '洛龙区', '偃师市', '孟津县', '新安县', '栾川县', '嵩县', '汝阳县', '宜阳县', '洛宁县', '伊川县'],
      '开封市': ['龙亭区', '顺河回族区', '鼓楼区', '禹王台区', '金明区', '杞县', '通许县', '尉氏县', '兰考县'],
      '平顶山市': ['新华区', '卫东区', '石龙区', '湛河区', '舞钢市', '汝州市', '宝丰县', '叶县', '鲁山县', '郏县'],
      '安阳市': ['文峰区', '北关区', '殷都区', '龙安区', '林州市', '安阳县', '汤阴县', '滑县', '内黄县'],
      '新乡市': ['红旗区', '卫滨区', '凤泉区', '牧野区', '卫辉市', '辉县市', '新乡县', '获嘉县', '原阳县', '延津县', '封丘县', '长垣市'],
      '焦作市': ['解放区', '中站区', '马村区', '山阳区', '沁阳市', '孟州市', '修武县', '博爱县', '武陟县', '温县'],
      '濮阳市': ['华龙区', '清丰县', '南乐县', '范县', '台前县', '濮阳县'],
      '许昌市': ['魏都区', '禹州市', '长葛市', '许昌县', '鄢陵县', '襄城县'],
      
      // 湖南省
      '长沙市': ['芙蓉区', '天心区', '岳麓区', '开福区', '雨花区', '望城区', '长沙县', '宁乡市', '浏阳市'],
      '株洲市': ['天元区', '荷塘区', '芦淞区', '石峰区', '渌口区', '醴陵市', '攸县', '茶陵县', '炎陵县'],
      '湘潭市': ['雨湖区', '岳塘区', '湘乡市', '韶山市', '湘潭县'],
      '衡阳市': ['珠晖区', '雁峰区', '石鼓区', '蒸湘区', '南岳区', '耒阳市', '常宁市', '衡阳县', '衡南县', '衡山县', '衡东县', '祁东县'],
      '邵阳市': ['双清区', '大祥区', '北塔区', '武冈市', '邵东县', '新邵县', '邵阳县', '隆回县', '洞口县', '绥宁县', '新宁县', '城步苗族自治县'],
      '岳阳市': ['岳阳楼区', '云溪区', '君山区', '汨罗市', '临湘市', '岳阳县', '华容县', '湘阴县', '平江县'],
      '常德市': ['武陵区', '鼎城区', '津市市', '安乡县', '汉寿县', '澧县', '临澧县', '桃源县', '石门县'],
      '张家界市': ['永定区', '武陵源区', '慈利县', '桑植县'],
      '益阳市': ['资阳区', '赫山区', '沅江市', '南县', '桃江县', '安化县'],
      
      // 福建省
      '福州市': ['鼓楼区', '台江区', '仓山区', '马尾区', '晋安区', '长乐区', '福清市', '闽侯县', '连江县', '罗源县', '闽清县', '永泰县', '平潭县'],
      '厦门市': ['思明区', '海沧区', '湖里区', '集美区', '同安区', '翔安区'],
      '莆田市': ['城厢区', '涵江区', '荔城区', '秀屿区', '仙游县'],
      '三明市': ['梅列区', '三元区', '永安市', '明溪县', '清流县', '宁化县', '大田县', '尤溪县', '沙县', '将乐县', '泰宁县', '建宁县'],
      '泉州市': ['鲤城区', '丰泽区', '洛江区', '泉港区', '石狮市', '晋江市', '南安市', '惠安县', '安溪县', '永春县', '德化县', '金门县'],
      '漳州市': ['芗城区', '龙文区', '龙海市', '云霄县', '漳浦县', '诏安县', '长泰县', '东山县', '南靖县', '平和县', '华安县'],
      '南平市': ['延平区', '建阳区', '邵武市', '武夷山市', '建瓯市', '顺昌县', '浦城县', '光泽县', '松溪县', '政和县'],
      '龙岩市': ['新罗区', '永定区', '漳平市', '长汀县', '上杭县', '武平县', '连城县'],
      '宁德市': ['蕉城区', '福安市', '福鼎市', '霞浦县', '古田县', '屏南县', '寿宁县', '周宁县', '柘荣县']
    })

    const cities = ref([])
    const areas = ref([])

    // 表单验证规则
    const rules = ref({
      receiver: [
        { required: true, message: '请输入收货人姓名', trigger: 'blur' }
      ],
      phone: [
        { required: true, message: '请输入联系电话', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入11位手机号码', trigger: 'blur' }
      ],
      province: [
        { required: true, message: '请选择省份', trigger: 'change' }
      ],
      city: [
        { required: true, message: '请选择城市', trigger: 'change' }
      ],
      area: [
        { required: true, message: '请选择区县', trigger: 'change' }
      ],
      detailAddress: [
        { required: true, message: '请输入详细地址', trigger: 'blur' }
      ]
    })

    const loadAddressList = async () => {
      try {
        const res = await getAddressList()
        addressList.value = res.data
      } catch (error) {
        console.error('获取地址列表失败', error)
        ElMessage.error('获取地址列表失败，请稍后重试')
      }
    }

    const handleAddAddress = () => {
      dialogTitle.value = '添加地址'
      addressForm.value = {
        id: '',
        receiver: '',
        phone: '',
        province: '',
        city: '',
        area: '',
        detailAddress: '',
        isDefault: 0
      }
      cities.value = []
      areas.value = []
      smartInput.value = ''
      dialogVisible.value = true
    }

    const handleEditAddress = (address) => {
      dialogTitle.value = '编辑地址'
      addressForm.value = {
        id: address.id,
        receiver: address.receiver,
        phone: address.phone,
        province: address.province,
        city: address.city,
        area: address.area,
        detailAddress: address.detailAddress,
        isDefault: address.isDefault
      }
      // 加载城市和区县数据
      if (address.province) {
        cities.value = cityData.value[address.province] || []
        if (address.city) {
          areas.value = areaData.value[address.city] || []
        }
      }
      smartInput.value = ''
      dialogVisible.value = true
    }

    const handleProvinceChange = (province) => {
      cities.value = cityData.value[province] || []
      addressForm.value.city = ''
      areas.value = []
      addressForm.value.area = ''
    }

    const handleCityChange = (city) => {
      areas.value = areaData.value[city] || []
      addressForm.value.area = ''
    }

    const smartRecognize = () => {
      const input = smartInput.value
      if (!input) {
        ElMessage.warning('请输入地址信息')
        return
      }

      // 重置表单，避免之前的识别结果干扰
      addressForm.value.receiver = ''
      addressForm.value.phone = ''
      addressForm.value.province = ''
      addressForm.value.city = ''
      addressForm.value.area = ''
      addressForm.value.detailAddress = ''
      cities.value = []
      areas.value = []

      // 匹配手机号（11位数字）
      const phoneMatch = input.match(/1[3-9]\d{9}/)
      if (phoneMatch) {
        addressForm.value.phone = phoneMatch[0]
      }

      // 匹配姓名（2-4个汉字，在手机号之前或开头）
      const nameMatch = input.match(/^[\u4e00-\u9fa5]{2,4}(?=\s|$|\d)/)
      if (nameMatch) {
        addressForm.value.receiver = nameMatch[0]
      }

      // 匹配地址（省份城市区县）
      const provinceRegex = /(北京市|上海市|广东省|江苏省|浙江省|山东省|四川省|湖北省|河南省|福建省|河北省|湖南省|安徽省|陕西省|黑龙江省|辽宁省|江西省|山西省|广西壮族自治区|云南省|内蒙古自治区|吉林省|贵州省|新疆维吾尔自治区|甘肃省|海南省|宁夏回族自治区|青海省|西藏自治区|香港特别行政区|澳门特别行政区|台湾省)/
      const provinceMatch = input.match(provinceRegex)
      
      if (provinceMatch) {
        addressForm.value.province = provinceMatch[1]
        handleProvinceChange(provinceMatch[1])
        
        // 匹配城市
        const cityRegex = new RegExp(provinceMatch[1] + '.*?(市|区|县|盟|州)')
        const cityMatch = input.match(cityRegex)
        if (cityMatch) {
          // 从匹配结果中移除省份名称
          let city = cityMatch[0].replace(provinceMatch[1], '').trim()
          // 确保城市名称与cityData中的城市名称匹配
          const cityList = cityData.value[provinceMatch[1]] || []
          const matchedCity = cityList.find(c => city.includes(c)) || city
          addressForm.value.city = matchedCity
          handleCityChange(matchedCity)
          
          // 匹配区县
          const areaRegex = new RegExp(matchedCity + '.*?(区|县|市|旗)')
          const areaMatch = input.match(areaRegex)
          if (areaMatch) {
            addressForm.value.area = areaMatch[0].replace(matchedCity, '')
            
            // 验证区县是否属于该城市
            const areaList = areaData.value[matchedCity] || []
            const isAreaValid = areaList.includes(addressForm.value.area)
            if (!isAreaValid) {
              ElMessage.error('地址识别错误：区县不属于该城市')
              // 重置表单
              addressForm.value.receiver = ''
              addressForm.value.phone = ''
              addressForm.value.province = ''
              addressForm.value.city = ''
              addressForm.value.area = ''
              addressForm.value.detailAddress = ''
              cities.value = []
              areas.value = []
              return
            }
          }
        }
      }

      // 提取详细地址（区县之后的内容）
      let detailAddress = input
      
      // 移除省份、城市、区县
      if (addressForm.value.area) {
        detailAddress = detailAddress.replace(addressForm.value.area, '').trim()
      }
      if (addressForm.value.city) {
        detailAddress = detailAddress.replace(addressForm.value.city, '').trim()
      }
      if (addressForm.value.province) {
        detailAddress = detailAddress.replace(addressForm.value.province, '').trim()
      }
      
      // 移除姓名和手机号
      if (addressForm.value.receiver) {
        detailAddress = detailAddress.replace(addressForm.value.receiver, '').trim()
      }
      if (addressForm.value.phone) {
        detailAddress = detailAddress.replace(addressForm.value.phone, '').trim()
      }
      
      // 移除可能的空格和特殊字符
      detailAddress = detailAddress.replace(/^\s*[\s,，]+\s*/, '').trim()
      
      if (detailAddress) {
        addressForm.value.detailAddress = detailAddress
      }

      // 验证是否成功识别出必要的地址信息
      if (!addressForm.value.province || !addressForm.value.city) {
        ElMessage.error('地址识别错误：无法识别完整的地址信息')
        // 重置表单
        addressForm.value.receiver = ''
        addressForm.value.phone = ''
        addressForm.value.province = ''
        addressForm.value.city = ''
        addressForm.value.area = ''
        addressForm.value.detailAddress = ''
        cities.value = []
        areas.value = []
        return
      }

      ElMessage.success('智能识别成功')
    }

    const handleSetDefault = async (id) => {
      try {
        // 这里需要调用设置默认地址的接口
        // 暂时使用updateAddress接口模拟
        const address = addressList.value.find(item => item.id === id)
        if (address) {
          await updateAddress({
            id: address.id,
            receiver: address.receiver,
            phone: address.phone,
            province: address.province,
            city: address.city,
            area: address.area,
            detailAddress: address.detailAddress,
            isDefault: 1
          })
          ElMessage.success('设置默认地址成功')
          loadAddressList()
        }
      } catch (error) {
        console.error('设置默认地址失败', error)
        ElMessage.error('设置默认地址失败，请稍后重试')
      }
    }

    const handleDeleteAddress = async (id) => {
      try {
        await ElMessageBox.confirm('确定要删除这个地址吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteAddress(id)
        ElMessage.success('删除地址成功')
        loadAddressList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除地址失败', error)
          ElMessage.error('删除地址失败，请稍后重试')
        }
      }
    }

    const submitForm = async () => {
      try {
        await addressFormRef.value.validate()
        if (addressForm.value.id) {
          // 编辑地址
          await updateAddress(addressForm.value)
          ElMessage.success('更新地址成功')
        } else {
          // 添加地址
          await addAddress(addressForm.value)
          ElMessage.success('添加地址成功')
        }
        dialogVisible.value = false
        loadAddressList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('保存地址失败', error)
          ElMessage.error('保存地址失败，请稍后重试')
        }
      }
    }

    onMounted(() => {
      loadAddressList()
    })

    return {
      addressList,
      dialogVisible,
      dialogTitle,
      addressForm,
      addressFormRef,
      smartInput,
      provinces,
      cities,
      areas,
      rules,
      handleAddAddress,
      handleEditAddress,
      handleProvinceChange,
      handleCityChange,
      smartRecognize,
      handleSetDefault,
      handleDeleteAddress,
      submitForm
    }
  }
}
</script>

<style scoped>
.address {
  width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.address h2 {
  margin-bottom: 20px;
}
</style>
