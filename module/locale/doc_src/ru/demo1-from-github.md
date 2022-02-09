<!--dolmen
label=Установка демо-модуля 
-->

- Создать новый проект для сборки Демо-модуля и получить исходый код с *GitHub*
    - запустить `c:/dolmen/workspace/idea.bat`
    - новый проект **User Defined | dolmen module | project name: demo1**
    - **View | Tool windows | Project**
    - **View | Appearance | Toolbar**
    - Подождать, когда закончится  Indexing JDK 12
    - **Build | Configure dolmen project**
    - удалить `module/module_mymodule.json`
    - удалить `src/com/mycompany/mymodule`
    - удалить `module.conf`
    - **VCS(Git) | enable Version Control Integration | Git**
    - **VCS(Git) | manage remotes,** добавить `https://github.com/oaslamov/demo.git`
    - **Login via GitHub**
    - В открывшемся окне браузера (Jetbrains) нажать **authorize in GitHub**
    - **VCS(Git) | Fetch**
    - **VCS(Git) | Branches | Remote branches | origin/master | checkout**

- Собрать модуль и запустить сервер
    - **Dolmen-deploy**
    - Подождать, когда закончится сборка
    - **Dolmen-tomcat**

- Установить модуль
    - зайти в веб-интерфейс
    - установить модуль *Demo module #1* в экране **Администрирование | Сервер и модули | Модули**

