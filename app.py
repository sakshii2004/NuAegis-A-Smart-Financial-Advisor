from flask import Flask, request, jsonify
import faiss
import json
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from transformers import AutoTokenizer, AutoModel
import torch
import numpy as np
from langchain.prompts import ChatPromptTemplate
from langchain.schema import HumanMessage
from langchain_google_genai import ChatGoogleGenerativeAI
import os

os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

app = Flask(__name__)
CORS(app) 
app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('DATABASE_URL')
db = SQLAlchemy(app)

class Summary(db.Model):
    __tablename__ = 'summaries'
    
    id = db.Column(db.Integer, primary_key=True)
    company = db.Column(db.String(50))
    year_quarter = db.Column(db.String(10))
    summary = db.Column(db.Text)

def get_summary(company, year_quarter):
    result = Summary.query.filter_by(company=company, year_quarter=year_quarter).with_entities(Summary.summary).all()
    summary = [row.summary for row in result]  # Extract summary field from each row
    return summary


class SentimentSummary(db.Model):
    __tablename__ = 'sentiment_summaries'
    
    id = db.Column(db.Integer, primary_key=True)
    company = db.Column(db.String(50))
    year_quarter = db.Column(db.String(10))
    pos_summary = db.Column(db.Text)
    neg_summary = db.Column(db.Text)

def get_positive_summary(company, year_quarter):
    result = SentimentSummary.query.filter_by(company=company, year_quarter=year_quarter).with_entities(SentimentSummary.pos_summary).all()
    positive_summary = [row.pos_summary for row in result if row.pos_summary]
    return positive_summary if positive_summary else []

def get_negative_summary(company, year_quarter):
    result = SentimentSummary.query.filter_by(company=company, year_quarter=year_quarter).with_entities(SentimentSummary.neg_summary).all()
    negative_summary = [row.neg_summary for row in result if row.neg_summary]
    return negative_summary if negative_summary else []


# Load FAISS index and metadata
faiss_index = faiss.read_index("faiss_embeddings.index")
with open("metadata.json", "r") as f:
    metadata_flat = json.load(f)

# Load tokenizer and model for FinBERT
tokenizer = AutoTokenizer.from_pretrained("yiyanghkust/finbert-tone")
embedding_model = AutoModel.from_pretrained("yiyanghkust/finbert-tone").to("cpu") 

# Set up the Gemini model with the API key
os.environ["GOOGLE_API_KEY"] = "AIzaSyBKcGRJxwinSLcqNz3koYk6vwNh9lWUY_o"  
gemini_model = ChatGoogleGenerativeAI(model="gemini-1.5-flash")

# Initialize the ChatPromptTemplate
chat_template = ChatPromptTemplate.from_template("""
You are a financial assistant specializing in analyzing earnings call data for various companies. 
Use the provided financial excerpts to answer the user's question as accurately as possible.

### *Guidelines for Answering:*
1. *Single Company Analysis*:
   - If a specific company and time period are requested (e.g., Amazon Q2 2021), summarize its financial performance.
   - Key metrics to highlight:
     - *Revenue*
     - *Earnings Per Share (EPS)*
     - *Operating Income*
     - *Net Income*
   - If no exact match is found, use the *nearest available* quarter or year.

2. *Comparative Analysis*:
   - If multiple companies are mentioned (e.g., Amazon vs. Walmart Q4 2020), present the financials *side by side*.
   - Structure responses like:
     - *Company 1:* Revenue: X, EPS: Y, Net Income: Z
     - *Company 2:* Revenue: A, EPS: B, Net Income: C
   - Use *nearest available data* if exact period data is missing.

3. *Investment Advice*:
   - If asked whether to invest in a company, summarize *recent financial performance* and give a *clear recommendation*.
   - Consider profitability trends, growth, and risks.

---
### *Financial Data Retrieved:*
{context}

### *User Question:*
{query}

### *Response:*
""")


# Define the embedding function
def embed_text(text):
    inputs = tokenizer(text, return_tensors='pt', truncation=True, max_length=512).to(device)
    with torch.no_grad():
        outputs = embedding_model(**inputs)  # Use `embedding_model` instead of `model`
    return outputs.last_hidden_state.mean(dim=1).squeeze().cpu().numpy()

def search_similar_chunks(query, top_k=5):
    # Generate the embedding for the query
    query_embedding = embed_text(query).reshape(1, -1)
    distances, indices = faiss_index.search(query_embedding, top_k * 2)  
    
    # Extract relevant keywords from the query
    query_keywords = query.lower().split()
    company_keywords = [word for word in query_keywords if word.isalpha()]  # Filter out words likely to be company names
    time_keywords = [word for word in query_keywords if word.startswith(('q', 'Q')) or word.isdigit()]  # Filter time references like Q2, 2022
    
    results = []
    for idx in indices[0]:
        if idx >= len(metadata_flat):
            continue  

        result = metadata_flat[idx]
        
        # Check if the result text contains company and time-related keywords
        if any(company_keyword in result['text'].lower() for company_keyword in company_keywords) and \
           any(time_keyword in result['text'] for time_keyword in time_keywords):
            results.append(result)
            if len(results) >= top_k:
                break
    
    return results


def generate_response(query, top_k=3):
    results = search_similar_chunks(query, top_k=top_k)
    context = "\n\n".join([result['text'] for result in results])
    prompt = chat_template.format(context=context, query=query)
    human_message = HumanMessage(content=prompt)
    response = gemini_model([human_message])
    return response.content

# default route for welcome message
@app.route('/')
def home():
    """Default route to confirm the API is running."""
    return jsonify({"message": "Welcome to the Financial Assistant API. Use the /search endpoint to submit queries."})

# define a route for similarity search with Gemini response generation
@app.route('/search', methods=['POST'])
def search():
    """API endpoint for similarity search on the FAISS index, using Gemini for response generation."""
    data = request.json
    query = data.get("query")
    
    # Check if query is provided
    if not query:
        return jsonify({"error": "No query provided"}), 400

    # Generate response based on the query
    response_content = generate_response(query)
    response_content = generate_response(query).replace("** ", "").replace("* ", "").replace("*", "").replace("**", "").replace("## ", "")
    return jsonify({"response": response_content})

# health check route
@app.route('/health', methods=['GET'])
def health_check():
    """Simple health check endpoint."""
    return jsonify({"status": "ok"})

@app.route('/summary', methods=['GET'])
def test():
    company = request.args.get('company')
    year_quarter = request.args.get('year_quarter')
    result = get_summary(company, year_quarter)
    if result:
        cleaned_text = result[0].replace('\\"', '"').replace('\\n', '\n')
        return jsonify(cleaned_text)
    else:
        fail_text = "No data found for " + company + " " + year_quarter[0:4] + " " + year_quarter[5:]
        return jsonify(fail_text)

@app.route('/summary_positive', methods=['GET'])
def summary_positive():
    company = request.args.get('company')
    year_quarter = request.args.get('year_quarter')
    result = get_positive_summary(company, year_quarter)
    return jsonify(result)


@app.route('/summary_negative', methods=['GET'])
def summary_negative():
    company = request.args.get('company')
    year_quarter = request.args.get('year_quarter')
    result = get_negative_summary(company, year_quarter)
    return jsonify(result)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
    



