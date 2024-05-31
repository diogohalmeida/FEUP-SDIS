# Distributed Backup Service for the Internet

O programa foi desenvolvido em Java 15.0.1 



## Instruções de Compilação

De modo a compilar o programa, é necessário correr o script **compile.sh** dentro da pasta **src** do seguinte modo: 

 `../scripts/compile.sh `



## Instruções de Execução

Para executar o programa, é necessário, primeiramente, iniciar o **rmiregistry** dentro da pasta **build**, da seguinte maneira: 

`start rmiregistry `

De seguida, também dentro da pasta **build**, iniciam-se os **peers**, através de:

` ../../scripts/peer.sh <peer_access_point> <peer_addr> <peer_port> <boot_addr> <boot_port>`

Por fim, dentro da pasta **build**, corre-se a **TestApp**, com:

`../../scripts/test.sh <peer_access_point> <subprotocol> <opnd1>* <opnd2>*`



## Instruções de Cleanup após a terminação do programa

Após terminar o programa, para limpar o diretório criado por cada **peer**, basta correr:

`../../scripts/cleanup.sh <peer_guid>`

