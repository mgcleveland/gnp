//
//  main.c
//  signalHandler
//
//  Created by Hin Lam on 9/22/13.
//  Copyright (c) 2013 Hin Lam. All rights reserved.
//

#include <stdio.h>
#include <signal.h>

void handler(int signo)
{
    printf ("signo = %d", signo);
    switch (signo)
    {
        case SIGINT:
            printf("CTRL-C pressed");
            break;
        default:
            printf ("Signal %d received", signo);
    }

}

int main(int argc, const char * argv[])
{

    if (signal(SIGINT, handler) == SIG_ERR)
    {
        printf("Can't register");
        
    }
    
    while (1)
    {
        
    }
    // insert code here...
    printf("Hello, World!\n");
    return 0;
}


