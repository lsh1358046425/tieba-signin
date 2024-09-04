# 简介
Github Actions实现百度贴吧自动签到。
项目参考 https://github.com/LuoSue/TiebaSignIn-1 进行重构。

# 使用
1.fork本项目

2.获取BDUSS

3.添加仓库Secrets

![image](https://github.com/user-attachments/assets/3c35bb39-4e4c-4347-9325-4fe4d46f649f)

4.开启actions

默认actions是处于禁止的状态，需要手动开启。

5.第一次运行actions

将`run.txt`中的`flag`由`0`改为`1`，push到自己的仓库。

```patch
- flag: 0
+ flag: 1
```
