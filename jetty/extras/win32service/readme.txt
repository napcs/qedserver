Jetty Service Instructions (For Wiki)

** installing jetty as windows service
    - in <jetty-root>/bin
    
    Jetty-Service.exe --install jetty-service.conf
        or
    Jetty-Service.exe -i jetty-service.conf
    
    
** Starting jetty as windows service
    - in <jetty-root>/bin
    
    Jetty-Service.exe --start jetty-service.conf
        or
    Jetty-Service.exe -t jetty-service.conf
    
    

    
** Stopping jetty as windows service
    - in <jetty-root>/bin
    
    Jetty-Service.exe --stop jetty-service.conf
        or
    Jetty-Service.exe -p jetty-service.conf
    
    

    
** Removing jetty as windows service
    - in <jetty-root>/bin
    
    Jetty-Service.exe --remove jetty-service.conf
        or
    Jetty-Service.exe -r jetty-service.conf
    