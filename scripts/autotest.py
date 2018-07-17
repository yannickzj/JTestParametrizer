import os
from subprocess import call

if __name__ == '__main__':

    ant_dir = '../../workspace/jfreechart-1.0.10/ant'
    result_dir = '../results/jfreechart-1.0.10'
    refactored_file = 'refactored.txt'
    testall = False

    fin = open(result_dir + '/' + refactored_file)
    lines = fin.readlines()

    os.chdir(ant_dir)

    if testall:
        call(['ant', 'test'])

    else:
        call(['ant', 'compile-tests'])
        for line in lines:
            components = line.split()
            call(['ant', '-Dtestclass=' + components[0], '-Dtestmethods=' + components[1], 'test-single'])

