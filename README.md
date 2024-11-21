可用的指令:

目前指令寫在 runFile.txt 裡, 使用前需要注意一下自己 Chrome 的版本與 pom.xml 裡的 driver 版本是否相容.

開啟某頁  
goPage -p { 網址 }  

重整頁面  
refresh 

點擊元素  
click by selector ( className/cssSelector/id/linkText/name/partialLinkText/tagName/xpath )  
click -b { 選擇器種類 } -s { 選擇器的值 }

等待頁面  
wait page  
wait -p { 網址 } optional: [ -r 等待網頁 ready / -l 等待網頁離開 ]

等待元素  
wait element by selector ( className/cssSelector/id/linkText/name/partialLinkText/tagName/xpath ) on condition ( clickable/visible/exist )  
wait -b { 選擇器種類 } -s { 選擇器的值 } -c { 條件的值 }

欄位設值  
set field by ( className/cssSelector/id/linkText/name/partialLinkText/tagName/xpath ) with value  
set field -b { 選擇器種類 } -s { 選擇器的值 } -v { 設定的值 }

Selenium 線程睡眠  
sleep in milliseconds  
sleep -t { 毫秒數 / 若沒有就是預設 1000 毫秒 }
