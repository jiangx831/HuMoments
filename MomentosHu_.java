import ij.*;
import java.lang.Object;
import java.io.*;
import java.io.IOException;
import ij.io.Opener;
import ij.io.OpenDialog;

/**
 * MomentosHU_ ImageJ plugin
 *
 * Este plugin abre uma imagem de referencia binarizada com
 * região interna em branco e externa em preto. Adicionalmente,
 * abre uma pasta onde está armazenada a base de imagens de
 * busca. Vetores de características são gerados para a imagem
 * de referência e para todas as imagens da base de busca.
 * Os vetores de características são armazenados em um arquivo
 * texto, onde cada linhacontém o nome do arquivo de uma
 * imagem, seguido do seu respectivo vetor de características.
 *
 *
 *
 * @author Julia Carmona
 * @author Náiron Galvez
 * @author Rodrigo Reis
 * @author Tais Ripa
 * @version 1.0 Nov 25, 2015.
 */

public class MomentosHu_  extends Object{

    //tamanho da imagem
    static float nx;
    static float ny;

	//acesso à imagem
        public static ImageAccess input;
	public static ImagePlus imp;

	//superfície e centro da gravidade
	private float m00;
	private float xg;
	private float yg;
	private String dir;

	//momentos invariantes de hu
	private float[] moments;
	private float[] momentsRef;
	private String[] result;
	private String [] resultRef;
	private String[] list;


	/**
	* Captura a imagem de referência aberta no ImageJ e
	* o diretório que contém as imagens de busca
	*/
	public MomentosHu_(){
		this.imp = WindowManager.getCurrentImage();
		this.moments = new float[7];
		this.input = new ImageAccess(imp.getProcessor().convertToByte(false));
		this.nx = input.getWidth();
		this.ny = input.getHeight();

		/* Somatória dos valores de luminância de todos os pixels */
		this.m00 = getM(0,0);

		/* Baricentro ou centro de massa do objeto */
		this.xg = getM(1,0)/m00;
		this.yg = getM(0,1)/m00;

		/* Imagem de referencia */
		this.momentsRef = new float[7];
		this.resultRef = new String [imp.getStackSize()];

		/* Pasta com a base de busca */
		OpenDialog od = new OpenDialog("Open search folder...");
		if (od.getFileName()==null) return;
		dir = od.getDirectory();
		allImages(dir);
	}


	/**
	* Calcula os momentos de Hu da imagem de referência e de todas as imagens de
	* um diretório especifico e utiliza a função save() para salvar os resultados
	* em um arquivo de texto
	*
	* @param dir		input, a String dir determina o nome do diretório de busca
	**/
	public void allImages(String dir){
		momentsRef = getHUsInvariantMoments(); /* Calcula os momentos da imagem de referencia */
		resultRef[0] = "Imagem Referencia: "+ "\n" + imp.getTitle() + " " + String.valueOf(momentsRef[0]) + " " + String.valueOf(momentsRef[1]) + " " + String.valueOf(momentsRef[2]) + " " + String.valueOf(momentsRef[3]) + " " + String.valueOf(momentsRef[4]) + " " + String.valueOf(momentsRef[5]) + " " + String.valueOf(momentsRef[6])+ "\n"+ "Imagens do Banco:";

		/* Trata o diretorio, e adiciona as imagens contidas nele numa lista */
		if (!dir.endsWith(File.separator))
            dir += File.separator;
        this.list = new File(dir).list();  /* lista de arquivos do diretorio */
		this.result = new String[list.length];
        if (list==null) {
			IJ.showMessage("Nenhuma imagem encontrada");
			return; /* Se nao houver nenhuma imagem no diretorio escolhido */
		}
		/* Calcula os momentos de hu para cada imagem contida no diretorio */
        for (int i=0; i<(list.length); i++) {
            File f = new File(dir+list[i]);
			if(!list[i].equals("Thumbs.db")) { /* Thumbs.db encontrado no sistema operacional Windows */
				if (!f.isDirectory()) {
					ImagePlus image = new Opener().openImage(dir, list[i]); /* abre imagem i da lista*/
					this.input = new ImageAccess(image.getProcessor().convertToByte(false));
					if (image != null) {
						this.nx = input.getWidth();
						this.ny = input.getHeight();
						this.m00 = getM(0,0);
						this.xg = getM(1,0)/m00;
						this.yg = getM(0,1)/m00;

						moments = getHUsInvariantMoments();
						result[i] = image.getTitle() + " " + String.valueOf(moments[0]) + " " + String.valueOf(moments[1]) + " " + String.valueOf(moments[2]) + " " + String.valueOf(moments[3]) + " " + String.valueOf(moments[4]) + " " + String.valueOf(moments[5]) + " " + String.valueOf(moments[6])+ " ";
					}
				}
			}
		}

		save(result, resultRef);
		IJ.showMessage("Done!");
	}


	/**
	* Calcula os momentos regulares Mpq de ordem 0 e ordem 1
	* sendo p,q = 0,1
	*
	* @param p       	ordem p
	* @param q       	ordem q
	*/
	public float getM(float p, float q){
		float value;
		float m = 0;
		for(int x = 0; x < nx; x++){
			for(int y = 0; y < ny; y++){
				value = (float) input.getPixel(x,y);
				if(value != 0){
					value = 1;
				}
				else{
					value = 0;
				}
				m += (float) Math.pow(x,p) * (float) Math.pow(y,q) * value;
			}

		}
		return m;
	}

	/**
	* Calcula os momentos centrais Npq, para que a rotação e a
	* translação sejam invariantes
	*
	* @param p       	ordem p
	* @param q       	ordem q
	*
	* @return Npq		momento central Npq
	*/
	public float getN(float p, float q){
		float n = 0;
		float value;
		for(int x = 0; x < nx; x++){
			for(int y = 0; y < ny; y++){
				value = (float) input.getPixel(x,y);
				if(value != 0){
					value = 1;
				}
				else{
					value = 0;
				}
				n += (float) Math.pow(( x - xg ), p) * (float) Math.pow(( y - yg ), q) * value;

			}

		}
		return n;
	}


	/**
	* Normaliza os momentos centrais em relacao ao tamanho da imagem,
	* tornando-os invariantes à escala
	*
	* @param p       	ordem p
	* @param q       	ordem q
	*
	* @return Npq		Npq normalizado
	*/
	public float getMi(float p, float q){

		float gama = ( p + q )/2 + 1;
		return (getN(p,q)/((float) Math.pow(getN(0,0),gama)));
	}


	/**
	* Calcular os 7 momentos invariantes de hu.
	* Os momentos são invariantes à escala, rotação e translação
	*
	* @return moments[]		vetor com 7 momentos de Hu
	**/
	public float[] getHUsInvariantMoments(){

		moments[0] = getMi(2, 0) + getMi(0, 2);
        moments[1] = (float) Math.pow((getMi(2, 0) - getMi(0, 2)), 2) + (float) Math.pow((2 * getMi(1, 1)), 2);
        moments[2] = (float) Math.pow((getMi(3, 0) - 3 * getMi(1, 2)), 2) + (float) Math.pow((3 * getMi(2, 1) - getMi(0, 3)), 2);
        moments[3] = (float) Math.pow((getMi(3, 0) + getMi(1, 2)), 2) + (float) Math.pow((getMi(2, 1) + getMi(0, 3)), 2);
        moments[4] = (getMi(3, 0) - 3 * getMi(1, 2)) * (getMi(3, 0) + getMi(1, 2)) * ( (float) Math.pow((getMi(3, 0) + getMi(1, 2)), 2) - 3 * (float) Math.pow((getMi(2, 1) + getMi(0, 3)), 2)) + (3 * getMi(2, 1) - getMi(0, 3)) * (getMi(2, 1) + getMi(0, 3)) * (3 * (float) Math.pow((getMi(0, 3) + getMi(1, 2)), 2) - (float) Math.pow((getMi(2, 1) + getMi(0, 3)), 2));
        moments[5] = (getMi(2, 0) - getMi(0, 2)) * ((float) Math.pow((getMi(3, 0) + getMi(1, 2)), 2)) - ((float) Math.pow((getMi(2, 1) + getMi(0, 3)), 2)) + 4 * getMi(1, 1) * (getMi(3, 0) + getMi(1, 2)) * (getMi(2, 1) + getMi(0, 3));
        moments[6] = (3 * getMi(2, 1) - getMi(0, 3)) * (getMi(3, 0) + getMi(1, 2)) * (float) Math.pow(((float) Math.pow((getMi(3, 0) + getMi(1, 2)), 2) - 3 * (getMi(2, 1) + getMi(0, 3))), 2) + (getMi(3, 0) - 3 * getMi(1, 2)) * (getMi(2, 1) + getMi(0, 3)) * (3 * ((float) Math.pow((getMi(3, 0) + getMi(1, 2)), 2)) - ((float) Math.pow((getMi(2, 1) + getMi(0, 3)), 2)));

		return moments;

	}


	/**
	* Salva os momentos invariantes da imagem em um arquivo.txt,
	* a partir de um fluxo de caracteres.
	**/
	public void save(String[] result,String[] resultRef){
		File arquivo = new File("ImageMoments.txt");
		try{
			FileWriter fw = new FileWriter(arquivo);
			fw.write(resultRef[0]+ "\n");
			for(int i = 0; i < result.length; i++){
				fw.write(result[i] + "\n");
			}
			fw.flush();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}

}
