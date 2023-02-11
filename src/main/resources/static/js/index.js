$(function(){
	$("#publishBtn").click(publish); // 给按钮绑定单击事件
});

function publish() {
	$("#publishModal").modal("hide");

	// // 发送 AJAX 请求之前，讲 CSRF 令牌设置到请求的消息头中
	// var token = $("meta[name='_csrf']").attr("content");
	// var header = $("meta[name='_csrf_header']").attr("content");
	// $(document).ajaxSend(function (e, xhr, options) {
	// 	xhr.setRequestHeader(header, token);
	// });

	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	$.post(
		CONTEXT_PATH + "/discuss/add", // 访问路径
		{"title":title, "content":content}, // 访问的数据
		function (data) { // 回调函数，处理返回结果
			data = $.parseJSON(data);
			// 在提示框当中显示返回的消息
			$("#hintBody").text(data.msg);
			// 显示提示框
			$("#hintModal").modal("show");
			// 两秒后自动隐藏
			setTimeout(function(){
				$("#hintModal").modal("hide");
				// 刷新页面
				if (data.code === 0) {
					window.location.reload();
				}
			}, 2000);
		}
	)
}