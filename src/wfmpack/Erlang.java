package wfmpack;

import java.util.HashMap;

public class Erlang {
	
	public static final double maxAccuracy = 0.00001;
	public static final int errorAgent = 1;
	public static final int errorSLA = 2;
	public static final int errorASA = 3;
	public static final int errorLines = 4;
	public static final int errorProductivity = 5;
	
	// declara��o dos atributos
	private int intervalInSeconds;
	private int necessaryAgents;
	private int insertedAgents;
	private double targetSLA;
	private double calcSLA;
	private int targetTime;
	private double calls;
	private double averageAnswerTime;
	private double nLines;
	private double intensity;
	private double calcWaitingTime;
	private double blockingPercentage;
	private double productivity;
	private HashMap<Integer, String> errors = new HashMap<Integer, String>();

	//declara��o dos contrutores
	/** Construtor b�sico para inicializa��o posterior dos valores 
	 * @return objeto do tipo Erlang para uso dos m�todos agent e SLA
	 */
	public Erlang() {
		this.load();
	}
	
	/** construtor espec�fico para iniciar calculando a quantidade de recursos necess�rios
	 * considerando as informa��es de N�vel de Servi�o, Tempo Aceit�vel e TMA
	 * 
	 * @param intervaloSegundos	int 	- determina o intervalo em segundos para uso nos c�lculos (ex. 900, 1800)
	 * @param SLAMeta	double	- indica qual o n�vel de servi�o dever� ser utilizado como meta no c�lculo de recursos
	 * @param tempoEsperaAceitavel	int	- define qual o tempo de resposta para contar uma chamada no n�vel de servi�o (at� quando pode ser respondida)
	 * @param chamadas 	double	- quantidade de chamadas esperadas/planejadas para o intervalo
	 * @param TMA	double	- tempo m�dio de atendimento planejado para o intervalo
	 * @return objeto Erlang com as informa��es preenchidas
	 */
	public Erlang( int intervaloSegundos, double SLAMeta, int tempoEsperaAceitavel, double chamadas, double TMA ){
		this.setIntervalInSeconds(intervaloSegundos);
		this.setTargetSLA(SLAMeta);
		this.setTargetTime(tempoEsperaAceitavel);
		this.setCalls(chamadas);
		this.setAverageAnswerTime(TMA);
		this.load();
	}
	
	/** Construtor espec�fico para iniciar calculando o n�vel de servi�o
	 *  considerando a quantidade de recursos informada
	 * @param intervaloSegundos, determina o intervalo em segundos para uso nos c�lculos (ex. 900, 1800)
	 * @param numAgentes, define qual o n�mero de agentes a ser utilizado no c�lculo do n�vel de servi�o
	 * @param tempoEsperaAceitavel, define qual o tempo de resposta para contar uma chamada no n�vel de servi�o (at� quando pode ser respondida)
	 * @param chamadas, quantidade de chamadas esperadas/planejadas para o intervalo
	 * @param TMA, tempo m�dio de atendimento planejado para o intervalo
	 * @return objeto Erlang
	 */
	public Erlang( int intervaloSegundos, int numAgentes, int tempoEsperaAceitavel, double chamadas, double TMA ){
		this.setIntervalInMinutes(intervaloSegundos);
		this.insertAgents(numAgentes);
		this.setTargetTime(tempoEsperaAceitavel);
		this.setCalls(chamadas);
		this.setAverageAnswerTime(TMA);
		this.load();
	}
	/** Construtor espec�fico para inicializar todos os atributos da classe
	 * @return objeto Erlang
	 */
	public Erlang( int interval, double sla, int acceptTime, double calls, double tma, int insAgents, double blocking ){
		this.setIntervalInSeconds(interval);
		this.setTargetSLA(sla);
		this.setTargetTime(acceptTime);
		this.setCalls(calls);
		this.setAverageAnswerTime(tma);
		this.insertAgents(insAgents);
		this.setBlockingPercentage(blocking);
		this.load();
	}
	
	// in�cio declara��o dos m�todos p�blicos
	/** exibe as informa��es em sa�da b�sica (texto... System.out.println)
	 * exibe o n�mero de recursos e o n�vel de servi�o
	 */
	public void exibir(){
		System.out.println( "Recurso = " + necessaryAgents );
		System.out.println( "Nivel de Servico = " + calcSLA );
	};

	/** Calcula o n�mero de recursos considerando as informa��es no objeto
	 * @return boolean: determina se conseguiu realizar a opera��o ou n�o
	 */
	protected boolean agent(){
		double birthRate, deathRate, trafficRate;
		double erlangs, utilisation, C, SLQueued;
		int maxIterate, count;
		int noAgents = 0;
		double server;

		if ( ( this.intervalInSeconds > 0 ) &&
				( this.targetSLA > 0 ) && 
				( this.targetTime > 0 ) && 
				( this.calls > 0 ) &&
				( this.averageAnswerTime > 0 ) ) {

			this.removeError(errorAgent);

			if (this.targetSLA > 1){
				this.targetSLA = 1;
			}
			
			birthRate = this.calls;
			deathRate = this.intervalInSeconds / this.averageAnswerTime;
	
			// calcula a intensidade de tr�fego
			trafficRate = birthRate / deathRate;
	
			// calcula o numero de Erlangs/horas
			erlangs = ( (int)(birthRate * (this.averageAnswerTime)) ) / this.intervalInSeconds + 0.5;
	
			// inicia o n�mero de agentes para 100% de uso
			if (erlangs < 1){
				noAgents = 1;
			}
			else {
				noAgents = ( (int)(erlangs) );
			}
			utilisation = trafficRate / noAgents;
			// agora busca o n�mero real e o n�mero abaixo de 100% de uso
			while (utilisation > 1){
				noAgents += 1;
				utilisation = trafficRate / noAgents;
			}
	
			maxIterate = noAgents * 100;
	
			for (count = 1 ; count <= maxIterate; count++){
				utilisation = trafficRate / noAgents;
				if (utilisation < 1){
					server = noAgents;
					C = erlangC(server, trafficRate);
	
					// encontra o n�vel do SLA com o n�mero de agentes informado
					SLQueued = 1 - C * ( Math.pow( Math.E, ((trafficRate - server) * this.targetTime / this.averageAnswerTime) ) ); // usa a constante de Euller
	
					if (SLQueued < 0){
						SLQueued = 0;
					}
					if (SLQueued >= this.targetSLA) {
						count = maxIterate;
					}
					// insere um limite na precis�o do SL (nunca ir� atingir 100%)
					if (SLQueued > (1 - maxAccuracy)) {
						count = maxIterate;
					}
				}
				if (count != maxIterate) {
					noAgents += 1;
				}
			}
		}
		else {
			// caso os par�metros n�o estejam preenchidos
			noAgents  = -1;
			this.insertError( errorAgent, "Informa��es incompletas para calcular agent.");
		}
		this.necessaryAgents = noAgents;
		return !this.hasError(errorAgent);
	}
	
	/** Calcula o n�mero de recursos considerando as informa��es de n�vel de servi�o e chamadas para o intervalo
	 * @param SLA	double	- indica qual o n�vel de servi�o dever� ser utilizado como meta no c�lculo de recursos
	 * @param serviceTime	int 	- define qual o tempo de resposta para contar uma chamada no n�vel de servi�o (at� quando pode ser respondida)
	 * @param callsPerHour	double	- quantidade de chamadas esperadas/planejadas para o intervalo
	 * @param AHT	double	- tempo m�dio de atendimento planejado para o intervalo
	 * @return agent	int	- n�mero de recursos calculados
	 */
	// utiliza o n�vel de servi�o planejado (SLA), o tempo aceit�vel de espera (serviceTime), 
	public int agent( double SLA, int serviceTime, double callsPerHour, double AHT ){
		int numberOfAgents = 0;
		this.setTargetSLA(SLA);
		this.setTargetTime(serviceTime);
		this.setCalls(callsPerHour);
		this.setAverageAnswerTime(AHT);
		
		if ( this.agent() ){
			numberOfAgents = this.getNecessaryAgents();
		}
		return numberOfAgents;
	}
	
	/** SLA: Calcula o n�vel de servi�o baseado nas informa��es no objeto
	 * @return boolean - determina se conseguiu calcular 
	 */
	//public double SLA( int agents, int serviceTime, double callsPerHour, double AHT ) { 
	protected boolean SLA() {
		double birthRate, deathRate, trafficRate, server;
		double utilisation, C, SLQueued;

		if (( this.insertedAgents > 0 ) && 
				( this.targetTime > 0 ) && 
				( this.calls > 0 ) && 
				( this.averageAnswerTime > 0) &&
				( this.intervalInSeconds > 0 ) ){

			this.errors.remove(errorSLA);
			
			birthRate = this.calls;
			deathRate = this.intervalInSeconds / this.averageAnswerTime;
	
			// calcula a intensidade de tr�fego
			trafficRate = birthRate / deathRate;
			utilisation = trafficRate / this.insertedAgents;
	
			if (utilisation >= 1)
				utilisation = 0.99;
	
			server = this.insertedAgents;
			C = erlangC(server, trafficRate);
	
			// Calcula o n�vel de servi�o considerando a fila e chamadas n�o enfileiradas
			// revisada a f�rmula com agradecimento a Tim Bolte e Jorn Lodahl pela ajuda/inser��o
			SLQueued = 1 - C * Math.pow( Math.E, (trafficRate-server)*this.targetTime / this.averageAnswerTime );
	
			this.calcSLA = minMax(SLQueued, 0, 1); // garante que o resultado esteja dentro dos limites
		}
		else {
			// caso par�metros n�o estejam preenchidos
			this.calcSLA = 0;
			this.insertError( errorSLA, "Par�metros insuficientes para calcular SLA.");
		}
			
		return !this.hasError();
	}
	
	/** SLA: Calcula o n�vel de servi�o baseado nos par�metros recebidos
	 * 
	 * @param agents	int	- n�mero de recursos base para c�lculo do n�vel de servi�o
	 * @param serviceTime	int - tempo aceit�vel de espera para c�lculo do n�vel de servi�o
	 * @param callsPerHour	double	- quantidade de chamadas projetada 
	 * @param AHT	double	- tempo m�dio de atendimento para o intervalo
	 * @return SLA	double	- valor calculado do n�vel de servi�o 
	 */
	public double SLA( int agents, int serviceTime, double callsPerHour, double AHT ) { 
		double SLA = 0.0;

		this.insertAgents(agents);
		this.setTargetTime(serviceTime);
		this.setCalls(callsPerHour);
		this.setAverageAnswerTime(AHT);
		
		if ( this.SLA() ){
			SLA = this.getSLA();
		}
		else {
			SLA = -1;
		}
		
		return SLA;
	}
	/**  nLines - calcula o n�mero de linha/troncos necess�rios considerando o % de bloqueio
	 * @param blocking double - % de chamadas que ser�o bloqueadas
	 * @return boolean determina se conseguiu calcular
	 */
	protected boolean nLines( ){
		double B = 0;
		double count = 0;
		double sngCount = 0;
		int maxIterate = 0;
		
		if ( this.intensity == 0 ) {
			this.intensity = ( this.calls * this.averageAnswerTime / this.intervalInSeconds );
		}
		
		if ( (this.intensity > 0) && (this.blockingPercentage > 0) ) {
			
			this.removeError(errorLines);
			
			maxIterate = 65535;
			
			for ( count = intCeiling( this.intensity ); count <= maxIterate; count++ ){
				sngCount = count;
				B = erlangB(sngCount, this.intensity);
				
				if ( B <= this.blockingPercentage)
					break;
			}
			
			if (count == maxIterate)
				count = 0;

			// retorna a quantidade identificada
			this.nLines = count;
		}
		else {
			this.nLines = -1;
			this.insertError(errorLines, "Par�metros insuficientes para calcular a quantidade de linhas");
		}
		
		return !this.hasError();
	}
	/**  nLines - calcula o n�mero de linha/troncos necess�rios considerando 
	 *  o % de bloqueio e a intensidade (chamadas x tma)
	 * @param intensity double - n�mero de erlangs para o intervalo
	 * @param blocking double - % de chamadas que ser�o bloqueadas
	 * @return nLines double - n�mero de linhas/troncos necess�rios
	 */
	public double nLines( double intensity, double blocking ){
		double nLines = -1;
		this.intensity = intensity;
		this.setBlockingPercentage(blocking);
		if ( !this.nLines() ){
			nLines = this.getLines();
		}
		return nLines;
	}
	/** ASA - calcula o tempo estimado de espera para o atendimento 
	 * @return boolean determina se conseguiu calcular ou n�o
	 */
	protected boolean ASA(){
		double birthRate = 0;
		double deathRate = 0;
		double trafficRate = 0;
		double utilisation = 0;
		double answerTime = 0;
		double aveAnswer = 0;
		double C = 0;
		double server = 0;
		
		if( (this.calls > 0) &&
				( this.intervalInSeconds > 0 ) && 
				( this.averageAnswerTime > 0 ) ) {
			
			this.removeError(errorASA);
			
			birthRate = this.calls;
			deathRate = this.intervalInSeconds / this.averageAnswerTime;
			
			// calcula a intensidade de tr�fego
			trafficRate = birthRate / deathRate;
			server = this.insertedAgents;
			utilisation = trafficRate / server;
			
			if (utilisation >= 1) {
				utilisation = 0.99;
			}
			
			C = erlangC(server, trafficRate);
			answerTime = C / ( server * deathRate * ( 1 - utilisation) );
			aveAnswer = secs(answerTime);
		}
		else {
			this.insertError(errorASA, "Par�metros insuficientes para calcular o ASA.");
		}
		this.calcWaitingTime = aveAnswer;
		return !this.hasError();
	}
	/** ASA - calcula o tempo estimado de espera para o atendimento 
	 * @param agents int - quantidade de agentes para o intervalo
	 * @param callsPerHour double - quantidade de chamadas para o intervalo
	 * @param AHT - tempo m�dio de atendimento planejado para o intervalo
	 * @return aveAnswer int - tempo de espera calculado
	 */
	public double ASA(int agents, double calls, double AHT){
		double aveAnswer = 0;
		this.insertAgents(agents);
		this.setCalls(calls);
		this.setAverageAnswerTime(AHT);
		
		if( !this.ASA() ){
			aveAnswer = this.getWaitingTime();
		}
		return aveAnswer;
	}
	/** hasError: indica se existe algum erro na carga ou preenchimento do objeto
	 * @return boolean, determina se existe erro ou n�o.
	 */
	public boolean hasError(){
		return ( !this.errors.isEmpty() );
	}
	/** hasError: indica se existe o erro espec�fico na carga ou preenchimento do objeto
	 * @param key, indica qual a chave a ser pesquisada
	 * @return boolean, determina se existe erro ou n�o.
	 */
	public boolean hasError( int key ){
		return ( this.errors.containsKey(key) );
	}
	
	//--------------------------------------------------------
	//  Setters e Getters // IN�CIO
	//--------------------------------------------------------
	/** setIntervalInMinutes: define o valor em minutos do intervalo para calcular os recursos e n�vel de servi�o
	 * tamb�m preenche o valor em segundos do intervalo
	 * @param minutos	int	- quantidade em minutos de dura��o do intervalo
	 */
	public void setIntervalInMinutes( int minutos ){
		intervalInSeconds = minutos * 60 ;
		return;
	}
	/** setIntervalInSeconds: define o valor em segundos do intervalo para calcular os recursos e n�vel de servi�o
	 * tamb�m preenche o valor em minutos do intervalo 
	 * @param minutos	int	- quantidade em segundos de dura��o do intervalo
	 */
	public void setIntervalInSeconds( int segundos ){
		intervalInSeconds = segundos;
		this.productivity();
		return;
	}
	/** getSecondsInterval: identifica a quantidade de segundos definida para os c�lculos
	 * @return segundosIntervalo	int - retorna o valor definido
	 */
	public int getSecondsInterval(){
		return intervalInSeconds;
	}
	/** setInsertedAgents: Define a quantidade de atendentes
	 * @param agents int - quantidade de agentes para o intervalo
	 */
	public void insertAgents( int agents ){
		this.insertedAgents = agents;
		this.load();
		return;
	}
	/** getInsertedAgents: Retorna a quantidade de atendentes
	 * @return agents int - quantidade de agentes para o intervalo
	 */
	public int getInsertedAgents(){
		return this.insertedAgents;
	}
	/** setTargetSLA: Define qual o n�vel de servi�o meta
	 * @param SLA double - N�vel de servi�o objetivo
	 */
	public void setTargetSLA( double sla ){
		this.targetSLA = sla;
		this.load();
		return;
	}
	/** getTargetSLA: Retorna qual o n�vel de servi�o meta definido
	 * @return targetSLA double - N�vel de servi�o objetivo
	 */
	public double getTargetSLA(){
		return this.targetSLA;
	}
	/** setTargetTime: Define qual o tempo aceit�vel de espera para o n�vel de servi�o
	 * @param time int - tempo aceit�vel de espera
	 */
	public void setTargetTime( int time ){
		this.targetTime = time;
		this.load();
		return;
	}
	/** getTargetTime: Retorna qual o tempo aceit�vel de espera para o n�vel de servi�o
	 * @return targetSLA double - tempo aceit�vel de espera
	 */
	public int getTargetTime(){
		return this.targetTime;
	}
	/** setCalls: Define qual a quantidade de chamadas para os c�culos
	 * @param calls double - quantidade de chamadas
	 */
	public void setCalls( double calls ){
		this.calls = calls;
		this.load();
		return;
	}
	/** getCalls: Retorna a quantidade de chamadas definida para os c�lculos
	 * @return calls double - quantidade de chamadas
	 */
	public double getCalls(){
		return this.calls;
	}
	/** setAverageAnswerTime: Define o tempo m�dio de atendimento
	 * @param aveTime double - tempo m�dio de atendimento
	 */
	public void setAverageAnswerTime( double aveTime ){
		this.averageAnswerTime = aveTime;
		this.load();
		return;
	}
	/** getAverageAnswerTime: Retorna o tempo m�dio de atendimento
	 * @return averageAnswerTime double - tempo m�dio de atendimento
	 */
	public double getAverageAnswerTime(){
		return this.averageAnswerTime;
	}
	/** setBlockingPercentage: Define o percentual de chamadas que poder�o ser bloqueadas
	 * @param blocking double - percentual de bloqueio
	 */
	public void setBlockingPercentage( double blocking ){
		this.blockingPercentage = blocking;
		this.load();
		return;
	}
	/** getBlockingPercentage: Retorna o percentual de bloqueio
	 * @return blockingPercentage double - percentual de bloqueio
	 */
	public double getBlockingPercentage(){
		return this.blockingPercentage;
	}
	/** getNecessaryAgents: Retorna o n�mero de recursos
	 * @return agenteEstimado	int - valor calculado de agentes usando o m�todo 'agent' 
	 */
	public int getNecessaryAgents() {
		return necessaryAgents;
	}
	/** getSLA: Retorna o n�vel de servi�o
	 * @return nivelServicoEstimado	double	- valor calculado usando o m�todo 'SLA'
	 */
	public double getSLA() {
		return calcSLA;
	}
	/** getWaitingTime: Retorna o tempo de espera em segundos para os dados da classe
	 * @return calcWaitingTime double - tempo de espera
	 */
	public double getWaitingTime(){
		return this.calcWaitingTime;
	}
	/** getLines: Retorna a quantidade de linhas calculados
	 * @return nLines double - tempo de espera
	 */
	public double getLines(){
		return this.nLines;
	}
	/** getProductivity: Retorna a produtividade para o intervalo considerando chamadas, tma e atendentes.
	 * @return SLA double - N�vel de servi�o objetivo
	 */
	public double getProductivity(){
		return this.productivity;
	}
	/** getErrors: Retorna o hash de erros
	 * @return errors HashMap - hash de erros
	 */
	public HashMap<Integer, String> getErrors(){
		return this.errors;
	}
	//--------------------------------------------------------
	//  Setters e Getters // FIM
	//--------------------------------------------------------
	
	//-----------------------------------------------------
	// in�cio declara��o dos m�todos private
	//-----------------------------------------------------
	/** calcula o erlangB
	 * @param servers int - n�mero de atendentes
	 * @param intensity - propor��o de chamadas x tempo m�dio de atendimento
	 * @return erlangB double - valor calculado de n�vel de servi�o
	 */
	private double erlangB(double servers, double intensity){
		double erlangB = 0.0;
		double val = 0;
		double last = 0;
		double B = 0;
		int count, maxIterate;

		if (servers > 0 && intensity > 0){
			maxIterate = (int)(servers);
			val = intensity;
			last = 1;
			for (count = 1; count <= maxIterate; count++){
				B = (val * last) / (count + (val * last));
				last = B;
			}
		}
		erlangB = minMax( B, 0, 1 );
		return erlangB;
	}
	
	/** calcula o erlangC
	 * @param servers int - n�mero de atendentes
	 * @param intensity - propor��o de chamadas x tempo m�dio de atendimento
	 * @return erlangC double - valor calculado de n�vel de servi�o
	 */
	private double erlangC(double servers, double trafficRate){ 
		double erlangC = 0.0;
		double B, C;

		if (servers > 0 && trafficRate > 0 ){
			B = erlangB(servers, trafficRate);
			C = B / (((trafficRate / servers ) * B ) + ( 1 - (trafficRate / servers ) ) );
			erlangC = minMax(C, 0, 1);
		}

		return erlangC;
	}
	
	/** restringe o resultado entre a faixa informada
	 * @param val double - valor a ser avaliado
	 * @param min double - limite inferior
	 * @param max double - limite superior
	 * @return minMax double - valor corrigido caso estivesse fora da faixa informada
	 */
	private double minMax(double val, double min, double max){	
		double minMax = val;

		if (val < min ){
			minMax = min;
		}
		else {
			if (val > max){
				minMax = max;
			}
		}
		return minMax;
	}
	/** intCeiling - arredonda para o n�mero maior mais pr�ximo
	 * 
	 * @param val double - valor a ser avaliado
	 * @return intCeiling double - valor alterado
	 */
	private double intCeiling( double val ){
		double intCeiling = 0;
		
		if ( val < 0 ){
			intCeiling = val - 0.9999;
		}
		else {
			intCeiling = val + 0.9999;
		}
		
		intCeiling = (int)(intCeiling);
		
		return intCeiling;
	}
	/** secs - converte um n�mero de horas em segundos
	 * @param amount double - 
	 * @return secs double - quantidade de segundos  
	 */
	private double secs( double amount ){
		return ( (amount * intervalInSeconds ) );
	}
	/** load - Realiza a carga das informa��es conforme os conte�dos v�o sendo inseridos no objeto
	 */
	private void load(){
		// calculando intensidade de tr�fego
		this.intensity();
		
		// calculando a produtividade
		this.productivity();
		
		// calculando os agentes necess�rios
		this.agent();
		
		// calculando o n�vel de servi�o para o intervalo
		this.SLA();
		
		// calculando o tempo de espera
		this.ASA();
		
		// calculando a quantidade de linhas/troncos
		this.nLines();
		return;
	}
	/** intensity - calcula a intensidade de tr�fego conforme chamadas, tma e intervalo s�o definidos
	 */
	private void intensity(){
		if ((this.calls >0) && 
				(this.averageAnswerTime > 0) &&
				(this.intervalInSeconds > 0)){
			
			this.intensity = ( this.calls * this.averageAnswerTime / this.intervalInSeconds );
		}
	}
	/** insertError - insere um erro na lista de erros da classe
	 * @param key, recebe a chave que o erro deve ser inserido. Quando a chave j� existe n�o insere novamente. 
	 * @param message, mensagem para a interpreta��o do erro.
	 */
	private void insertError( int key, String message ){
		if (!this.errors.containsKey(key)){
			this.errors.put(key, message);
		}
	}
	/** removeError - remove um erro do mapeamento dos erros
	 * @param 	key, int - chave para a busca e remo��o do erro.
	 */
	private void removeError( int key ){
		if (this.errors.containsKey(key)){
			this.errors.remove(key);
		}
	}
	/** productivity - calcula a produtividade conforme (chamadas * TMA * Agentes / segundos do intervalo).
	 */
	private void productivity(){
		if (this.intervalInSeconds > 0){
			// calcula a produtividade
			this.productivity = ( ( this.calls * this.averageAnswerTime ) / ( this.insertedAgents * this.intervalInSeconds) );
			// limita o resultado a 100%
			if (this.productivity > 1){
				this.productivity = 1;
			}
			this.removeError(errorProductivity);
		}
		else {
			this.insertError(errorProductivity, "Par�metros insuficientes para calcular a produtividade.");
		}
	}
	
	/** finalize: Finaliza o objeto e limpa os dados de erros.
	 */
	protected void finalize(){
		this.errors.clear();
	}

}
