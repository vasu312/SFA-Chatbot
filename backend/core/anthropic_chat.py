import re
from typing import List

import anthropic

from vanna.base import VannaBase


class Anthropic_Chat(VannaBase):
    """Vanna LLM backend using Anthropic Claude."""

    def __init__(self, config=None):
        VannaBase.__init__(self, config=config)
        if config is None:
            raise ValueError("config dict with 'api_key' and 'model' is required")
        self._client = anthropic.Anthropic(api_key=config["api_key"])
        self._model = config.get("model", "claude-sonnet-4-20250514")

    # --- message helpers ---

    @staticmethod
    def system_message(message: str) -> dict:
        return {"role": "system", "content": message}

    @staticmethod
    def user_message(message: str) -> dict:
        return {"role": "user", "content": message}

    @staticmethod
    def assistant_message(message: str) -> dict:
        return {"role": "assistant", "content": message}

    @staticmethod
    def str_to_approx_token_count(string: str) -> int:
        return len(string) // 4

    # --- prompt builders (copied from OpenAI_Chat) ---

    @staticmethod
    def add_ddl_to_prompt(initial_prompt: str, ddl_list: List[str], max_tokens: int = 14000) -> str:
        if ddl_list:
            initial_prompt += "\nYou may use the following DDL statements as a reference for what tables might be available. Use responses to past questions also to guide you:\n\n"
            for ddl in ddl_list:
                if Anthropic_Chat.str_to_approx_token_count(initial_prompt) + Anthropic_Chat.str_to_approx_token_count(ddl) < max_tokens:
                    initial_prompt += f"{ddl}\n\n"
        return initial_prompt

    @staticmethod
    def add_documentation_to_prompt(initial_prompt: str, documentation_list: List[str], max_tokens: int = 14000) -> str:
        if documentation_list:
            initial_prompt += "\nYou may use the following documentation as a reference for what tables might be available. Use responses to past questions also to guide you:\n\n"
            for doc in documentation_list:
                if Anthropic_Chat.str_to_approx_token_count(initial_prompt) + Anthropic_Chat.str_to_approx_token_count(doc) < max_tokens:
                    initial_prompt += f"{doc}\n\n"
        return initial_prompt

    @staticmethod
    def add_sql_to_prompt(initial_prompt: str, sql_list: List[str], max_tokens: int = 14000) -> str:
        if sql_list:
            initial_prompt += "\nYou may use the following SQL statements as a reference for what tables might be available. Use responses to past questions also to guide you:\n\n"
            for question in sql_list:
                if Anthropic_Chat.str_to_approx_token_count(initial_prompt) + Anthropic_Chat.str_to_approx_token_count(question["sql"]) < max_tokens:
                    initial_prompt += f"{question['question']}\n{question['sql']}\n\n"
        return initial_prompt

    def get_sql_prompt(self, question: str, question_sql_list: list, ddl_list: list, doc_list: list, **kwargs):
        initial_prompt = (
            "The user provides a question and you provide SQL. "
            "You will only respond with SQL code and not with any explanations.\n\n"
            "Respond with only SQL code. Do not answer with any explanations -- just the code.\n"
        )
        initial_prompt = self.add_ddl_to_prompt(initial_prompt, ddl_list)
        initial_prompt = self.add_documentation_to_prompt(initial_prompt, doc_list)

        message_log = [self.system_message(initial_prompt)]
        for example in question_sql_list:
            if example and "question" in example and "sql" in example:
                message_log.append(self.user_message(example["question"]))
                message_log.append(self.assistant_message(example["sql"]))
        message_log.append(self.user_message(question))
        return message_log

    def get_followup_questions_prompt(self, question: str, df, question_sql_list: list, ddl_list: list, doc_list: list, **kwargs):
        initial_prompt = f"The user initially asked the question: '{question}': \n\n"
        initial_prompt = self.add_ddl_to_prompt(initial_prompt, ddl_list)
        initial_prompt = self.add_documentation_to_prompt(initial_prompt, doc_list)
        initial_prompt = self.add_sql_to_prompt(initial_prompt, question_sql_list)

        message_log = [self.system_message(initial_prompt)]
        message_log.append(self.user_message(
            "Generate a list of followup questions that the user might ask about this data. "
            "Respond with a list of questions, one per line. Do not answer with any explanations -- just the questions."
        ))
        return message_log

    def generate_question(self, sql: str, **kwargs) -> str:
        return self.submit_prompt([
            self.system_message(
                "The user will give you SQL and you will try to guess what the business question this query is answering. "
                "Return just the question without any additional explanation. Do not reference the table name in the question."
            ),
            self.user_message(sql),
        ])

    def submit_prompt(self, prompt, **kwargs) -> str:
        if not prompt:
            raise ValueError("Prompt is empty")

        # Separate system message from conversation messages
        system_text = ""
        messages = []
        for msg in prompt:
            if msg["role"] == "system":
                system_text += msg["content"] + "\n"
            else:
                messages.append({"role": msg["role"], "content": msg["content"]})

        # Anthropic requires at least one user message
        if not messages:
            messages = [{"role": "user", "content": system_text.strip()}]
            system_text = ""

        response = self._client.messages.create(
            model=self._model,
            max_tokens=1024,
            system=system_text.strip() if system_text else anthropic.NOT_GIVEN,
            messages=messages,
        )
        return response.content[0].text
