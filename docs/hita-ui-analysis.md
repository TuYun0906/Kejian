# HITA UI静态分析与应用改进

日期：2026-11-05。目标：用户提供的F:/Downloads/HITA.apk.1。仅静态检查，未安装或运行。

## REA证据
APK SHA256：b59266776a83125f955ed7cfe32fc0ad8ee77bed052c68535e9ec8d1fc3c8d82。
REA inspect_artifact完整父证据：ev_fed4b3f42aa363308a8b6d8bfde7b24d3e35b9614d51ed0291bbc604ab2f8cba。
摘要证据：ev_22d5bb379141dde48374dbb9b6e38eb178fb59aa19c32167886526fd274c558e。
清单包含5727个文件、0个已报告完整性矛盾。APK.1原始扩展名先被识别为ZIP，复制到工作目录以.apk后缀分析，原文件未修改。

inspect_android_package因缺少调用方提供的JADX MCP JAR不可用，因此未取得Java/Kotlin反编译结果。随后采用REA资源清单和官方aapt2解码资源表及指定布局，未宣称完成代码或运行时逆向。

## 已观察资源
- color/surface为#f6f7fa，surface_variant白色；on_surface黑色，次级文字灰色；包含夜间变体。
- ds_course_*引用9种subject色：绿色、粉色、橙色、紫色、红色、靛色、蓝色、青色、青绿色。
- corner_*包含4/8/12/16/24dp等规格；space_*主要采用4dp级差。
- ds_timeline_width、timeline_margin_top、timeline_card_elevation等资源支持时间轴排版的存在；资源名本身不能证明实际交互。
- layout/widget_today_item映射res/Jc.xml，解码观察到时间/地点相关横向容器、12dp图标、12/13sp文字及紧凑内边距。此处是桌面小组件布局，不能直接当作应用主页面运行截图。

## 自主实现的改进
采用统一浅色背景、白色与课程淡色卡片、16/24dp圆角、紧凑地点徽标及4dp级差间距；保留此前已确认的圆形进度、最近已结束课程低亮和一屏周课表。

新增CoursePalette：按当前学期课程名称集合确定颜色，同名课程在今日、顶部状态和周课表保持一致；不同课程不再用5色hash取模而产生常见碰色。测试42个不同名称（含Java哈希碰撞Aa/BB）区分颜色，并验证输入顺序不影响结果。新增课程名称可能引起排序及色相重分配，目前没有独立的自定义/持久色板设置。

缺失地点不显示“待定”；今日地点徽标隐藏，周课表地点区域留空。未拷贝HITA图标、字体、背景图、业务代码。

## 验证与未知
已有时间、数据、导入、认证脚本测试通过；新增配色测试通过；APK源码编译、DEX打包和签名验证通过。尚未运行HITA或本应用进行安卓实机视觉对照，登录及手机平板布局仍需实机验证。静态资源分析不证明运行时动效、布局组合及自动配色算法。
