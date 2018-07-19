import os
from subprocess import call

if __name__ == '__main__':

    jfreechart_dir = '../../workspace/jfreechart-1.0.10-rf'
    ant_dir = './ant'
    result_dir = '../results/jfreechart-1.0.10'
    refactored_file = 'refactored.txt'
    testall = True

    fin = open(result_dir + '/' + refactored_file)
    lines = fin.readlines()

    os.chdir(jfreechart_dir)
    #print('remove jar files and build folders')
    #call(['rm', '-rf', 'jfreechart-1.0.10-experimental.jar', 'jfreechart-1.0.10.jar', 'build-tests', 'build-tests-reports'])

    os.chdir(ant_dir)
    if testall:
        call(['ant', 'test'])

    else:
        call(['ant', 'compile-tests'])
        for line in lines:
            components = line.split()
            call(['ant', '-Dtestclass=' + components[0], '-Dtestmethods=' + components[1], 'test-single'])
