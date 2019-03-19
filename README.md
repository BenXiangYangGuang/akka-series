### Akka Stream 理解
[由来](https://www.oschina.net/translate/a-gentle-introduction-to-akka-streams)  
[主要组件](https://juejin.im/post/5ae05683518825673f0b4bb4)  
[官方教程](https://www.blog-china.cn/blog/liuguobing/home/91/1492250104349)
[官方教程响应式推文](https://www.cnblogs.com/devos/p/4456582.html)
#### akka Stream 设计实施原则  
- 所有功能都在API中明确，没有魔力  
- 最高的组合性：组合件保留了每个部件的功能
- 分布式有界流处理领域的详尽模型
> 背压 缓冲   
> 和响应式流的解耦 相互操作 api易用性   
> 流式处理图的定义 组件的定义 可重用行
> 错误处理 故障恢复
---
- 服务器端自动负载均衡
> 客户端发送请求,每一个服务器端处理得到请求不一样.自动分配;不重复