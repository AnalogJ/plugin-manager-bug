pipelineJob('example') {
    displayName('Job DSL Example Project')
    description('My first job')
    logRotator(30, -1, 1, -1)
}

//job('example') {
//    displayName('Job DSL Example Project')
//    description('My first job')
//    logRotator(30, -1, 1, -1)
//    steps {
//        shell('echo Hello World!')
//    }
//}