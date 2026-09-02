<template>
  <footer>
    <div class="container">
      <div class="footer-content">
        <div class="footer-section">
          <h3>关于我们</h3>
          <p>猫猫百货商城</p>
          <p>一家专注于专注于提供优质商品的在线购物平台</p>
        </div>
        <div class="footer-section">
          <h3>联系方式</h3>
          <p>电话：13226430986</p>
          <p>邮箱：3088238757@qq.com</p>
        </div>
        <div class="footer-section">
          <h3>快速链接</h3>
          <ul>
            <li><a href="/">首页</a></li>
            <li><a href="/product">商品</a></li>
            <li><a href="/user">个人中心</a></li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 猫猫百货商城. 保留所有权利</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
footer {
  background: #333;
  color: #fff;
  padding: 30px 0;
  margin-top: 50px;
}

.container {
  width: 1200px;
  margin: 0 auto;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
}

.footer-section {
  flex: 1;
  margin: 0 20px;
}

.footer-section h3 {
  margin-bottom: 20px;
}

.footer-section ul {
  list-style: none;
}

.footer-section ul li {
  margin: 10px 0;
}

.footer-section ul li a {
  color: #fff;
  text-decoration: none;
}

.footer-bottom {
  text-align: center;
  border-top: 1px solid #555;
  padding-top: 20px;
}
</style>
