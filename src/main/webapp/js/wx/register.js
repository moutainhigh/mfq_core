// JavaScript Document

//发送验证码

var InterValObj; //timer变量，控制时间
var count = 60; //间隔函数，1秒执行
var curCount;//当前剩余秒数
			
jQuery.noConflict();
(function (win, doc, $) {
	
	var register={
		init:function(){
			var _this = register;	
			
			$(".quren").on('click', function(){
				_this.__validationForm();
				if(!_this.__checkForm()){
					return false;  
				}
				$("#reg").submit();
			});
			
			$("#btnSendCode").on('click',function(){
				
				var verifyCode = $("#uid").val();
				var pwd=$.trim($(".passwd").val());
				var umobile=$.trim($(".us_phone").val());
				
				if(!_this.__isMobile(umobile)){
					$("#phone-error").html("手机号非法！！");
					return false;
				}
				
				var data=_this.__isUsePhone(umobile,verifyCode);
				if(data.code!=0){
					location.href ="/wechat/reg_ret?code="+data.code+"&msg="+data.msg;
					return false;
				}
				var vdata=_this.__sendVcode(umobile,verifyCode)
				if(vdata.code!=0){
					$("#vcode-error").html(vdata.msg);
				}
			});
			
			//获取焦点充值错误提醒
			$(".passwd").focus(function(){
				$("#pwd-error").html("");
			});
			$(".us_phone").focus(function(){
				$("#phone-error").html("");
			});
			
			$(".v_code").focus(function(){
			//	$("#vcode-error").html("");
			});
			
		},
		__sendVcode:function(mobile,vialcode){
			  var ret;
			  curCount = count;			
			  mobile = $(".us_phone").val();
			  u = $("#uid").val();
			  
				//向后台发送处理数据
			  $.ajax({
			    type: "GET", //用POST方式传输
			    async:false, 
			    url: thisapp+'/wechat/mcode/', //目标地址
			    dataType:"json",
			    data: {mobile:mobile,u:u,a:Math.random()+100},
			    success: function (data){
			    	if(data.code==0){
			    	 //设置button效果，开始计时
			   	     $("#btnSendCode").attr("disabled", "true").css("background-color","#F7F7F7");
			   	     $("#btnSendCode").val(curCount + "秒");
			   	     InterValObj = window.setInterval(register.__SetRemainTime, 1000); //启动计时器，1秒执行一次
			    	}
			    	ret=data;
			      }
			     });
			  return ret;
			
		},
		__SetRemainTime:function(){ //timer处理函数
			if (curCount == 0) {                
                window.clearInterval(InterValObj);//停止计时器
                $("#btnSendCode").removeAttr("disabled").css("background-color","#CC76CB");//启用按钮
                $("#btnSendCode").val("重新发送");
            }
            else {
                curCount--;
                $("#btnSendCode").val(curCount + "秒");
            }
		},
		__validationForm:function(){
			var verifyCode = $("#uid").val();
			var pwd=$.trim($(".passwd").val());
			var umobile=$.trim($(".us_phone").val());
		
			if(pwd.length < 6){
				
				$("#pwd-error").html("密码格式不对！！");
				return false;
			}
			
			if(!register.__isMobile(umobile)){
				$("#phone-error").html("手机号非法！i！");
			}
			
			var v_code=$.trim($(".v_code").val());
			if(v_code.length!=4){
				$("#vcode-error").html("验证码错误！！");
			}
			var vdata=register.__checkVCode(umobile,v_code,verifyCode);
			if(vdata.code!=0){
				$("#vcode-error").html(vdata.msg);
				return false;
			}
			
		},
		__checkForm:function(){
			var verifyCode = $("#uid").val();
			var pwd=$.trim($(".passwd").val());
			var umobile=$.trim($(".us_phone").val());
			if(pwd.length < 6){
				$("#pwd-error").html("密码格式不对！！");
				return false;
			}
			
			if(!register.__isMobile(umobile)){
				$("#phone-error").html("手机号非法！！");
				return false;
			}
			
			var v_code=$.trim($(".v_code").val());
			if(v_code.length!=4){
				$("#vcode-error").html("验证码错误！！");
				return false;
			}
			var vdata=register.__checkVCode(umobile,v_code,verifyCode);
			if(vdata.code!=0){
				$("#vcode-error").html(vdata.msg);
				return false;
			}
			return true;
			
		},
		__isMobile:function(value){
			var length = value.length;    
		    return length == 11 && /^(1+\d{10})$/.test(value);
		},
		__isUsePhone:function(mobile,verifyCode){ //是否可用手机号
			var ret;
			//验证手机号是否可用
			//var tp=false;
			$.ajax({
				    type: "GET", //用GET方式传输
				    url: thisapp+'/wechat/mobile/check/?phone='+mobile+'&u='+verifyCode, //目标地址
				    dataType:"json",
				    async:false,
				    success: function(data){
				    	ret=data;
				    }
			 });
			 return ret;
		},
		__checkVCode:function(mobile,vcode,vercode){
		    
		  var ret;
		  //验证手机号是否可用
		  var tp=false;
		  $.ajax({
			     type: "GET", //用POST方式传输
			     url: thisapp+'/wechat/check/', //目标地址
			     dataType:"json",
			     async:false,
			     data: {                     //要传递的数据
				    	mobile: mobile,
				    	vcode: vcode,
				        u:vercode
				    },
			    success: function(data){
			    	ret=data;
			    }
			    
		  });
		  return ret;
		
		
		}
		
		
			
	}
	
	win.userRegister = register.init;

})(window, document, jQuery);
